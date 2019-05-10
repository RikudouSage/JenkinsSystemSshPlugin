package cz.chrastecky.SystemSshPlugin;

import cz.chrastecky.SystemSshPlugin.System.FilePermissions;
import cz.chrastecky.SystemSshPlugin.VO.SshServer;
import hudson.Launcher;
import hudson.Proc;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

class SshClient {

    private final SshServer server;
    private final SshClientConfig config;
    private final Launcher launcher;

    SshClient(@NotNull SshServer server, Launcher launcher, SshClientConfig config) {
        this.server = server;
        this.config = config;
        this.launcher = launcher;
    }

    @NotNull
    private String getConnectCommand() {
        return "setsid -w ssh -o StrictHostKeyChecking=no -o IdentitiesOnly=yes -n -i '"
                + server.getSshKey()
                + "' -p " + server.getPort()
                + " " + server.getUsername()
                + "@" + server.getHostname()
                + " < /dev/null";
    }

    @NotNull
    private String getCommand(@NotNull String command) {
        return getConnectCommand() + " '" + command.replace("'", "\\'") + "'";
    }

    int executeCommand(@NotNull String command, @NotNull PrintStream output) {
        File askPassFile = new File("askpass.sh");
        int exitCode;
        try {
            output.println();
            output.print("[SSH Plugin] $ ");
            if (!config.shouldHideCommand()) {
                output.println(command);
            } else {
                output.println("***command hidden***");
            }
            String sshCommand = getCommand(command);

            Map<String, String> environment = new HashMap<>();
            environment.put("SSH_AUTH_SOCK", "");
            environment.put("SSH_AGENT_PID", "");

            Launcher.ProcStarter starter = launcher
                    .launch()
                    .cmdAsSingleString(sshCommand)
                    .readStdout()
                    .stdin(new FileInputStream("/dev/null"))
                    .quiet(true);

            if (server.getPassword() != null) {
                if (!askPassFile.exists() && !askPassFile.createNewFile()) {
                    throw new Exception("Could not create the file for password");
                }

                Files.setPosixFilePermissions(
                        askPassFile.toPath(),
                        new FilePermissions(
                                PosixFilePermission.OWNER_READ,
                                PosixFilePermission.OWNER_WRITE,
                                PosixFilePermission.OWNER_EXECUTE
                        )
                );

                try (FileWriter writer = new FileWriter(askPassFile)) {
                    writer.write("#!" + config.getLocalShell() + "\necho '" + server.getPassword() + "'\n");
                }

                environment.put("DISPLAY", ":");
                environment.put("SSH_ASKPASS", askPassFile.getAbsolutePath());
            }

            starter.envs(environment);

            Proc process = starter.start();
            InputStream stdout = process.getStdout();
            if (stdout != null) {
                String line;
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stdout));
                while ((line = bufferedReader.readLine()) != null) {
                    if (!config.shouldHideOutput()) {
                        output.println("[SSH Plugin] " + line);
                    }
                }
                if (config.shouldHideOutput()) {
                    output.println("[SSH Plugin] ***command output hidden***");
                }
                output.println();
            }

            exitCode = process.join();
            process.kill();
        } catch (Exception e) {
            e.printStackTrace();
            exitCode = -1;
        } finally {
            if (askPassFile.exists()) {
                if (!askPassFile.delete()) {
                    Logger.getLogger(getClass().toString())
                            .warning("Could not delete file '" + askPassFile.getAbsolutePath() + "', SSH key password is exposed");
                    if (askPassFile.canWrite()) {
                        try (FileWriter writer = new FileWriter(askPassFile)) {
                            writer.write("");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return exitCode;
    }

}
