package cz.chrastecky.SystemSshPlugin;

public class SshClientConfig {

    private String localShell;
    private boolean hideOutput;
    private boolean hideCommand;

    SshClientConfig() {
        this("/bin/sh", false, false);
    }

    public SshClientConfig(String localShell, boolean hideOutput, boolean hideCommand) {
        this.localShell = localShell;
        this.hideOutput = hideOutput;
        this.hideCommand = hideCommand;
    }

    String getLocalShell() {
        return localShell;
    }

    public void setLocalShell(String localShell) {
        this.localShell = localShell;
    }

    public boolean shouldHideOutput() {
        return hideOutput;
    }

    public void setHideOutput(boolean hideOutput) {
        this.hideOutput = hideOutput;
    }

    public boolean shouldHideCommand() {
        return hideCommand;
    }

    public void setHideCommand(boolean hideCommand) {
        this.hideCommand = hideCommand;
    }
}
