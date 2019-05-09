package cz.chrastecky.SystemSshPlugin;

import cz.chrastecky.SystemSshPlugin.VO.SshServer;
import hudson.EnvVars;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Map;

class SshClient {

    private final SshServer server;
    private final EnvVars envVars;
    private final SshClientConfig config;

    SshClient(@NotNull SshServer server, EnvVars envVars, SshClientConfig config) {
        this.server = server;
        this.envVars = envVars;
        this.config = config;
    }

    private String getConnectCommand() {
        return "ssh -i '" + server.getSshKey() + "' -p " + server.getPort() + " " + server.getUsername() + "@" + server.getHostname();
    }

    private String getCommand(@NotNull String command) {
        return getConnectCommand() + " '" + command.replace("'", "\\'") + "'";
    }

    int executeCommand(@NotNull String command, @NotNull PrintStream output) {
        try {
            output.println();
            output.print("[SSH Plugin] $ ");
            if(!config.shouldHideCommand()) {
                output.println(command);
            } else {
                output.println("***command hidden***");
            }
            String sshCommand = getCommand(command);
            ProcessBuilder processBuilder = new ProcessBuilder(config.getLocalShell(), "-c", sshCommand);
            processBuilder.redirectErrorStream(true);

            Map<String, String> environment = processBuilder.environment();
            for(String key: envVars.keySet()) {
                String value = envVars.get(key);
                environment.put(key, value);
            }

            Process process = processBuilder.start();

            String line;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = bufferedReader.readLine()) != null) {
                if(!config.shouldHideOutput()) {
                    output.println("[SSH Plugin] " + line);
                }
            }
            if(config.shouldHideOutput()) {
                output.println("[SSH Plugin] ***command output hidden***");
            }
            output.println();
            process.waitFor();
            int exitCode = process.exitValue();
            process.destroy();

            return exitCode;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

    }

}
