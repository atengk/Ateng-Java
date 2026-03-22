package io.github.atengk.sshj.core;

public class SshCommandResult {

    private final String stdout;
    private final String stderr;
    private final Integer exitStatus;

    public SshCommandResult(String stdout, String stderr, Integer exitStatus) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitStatus = exitStatus;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public Integer getExitStatus() {
        return exitStatus;
    }

    @Override
    public String toString() {
        return "SshCommandResult{" +
                "stdout='" + stdout + '\'' +
                ", stderr='" + stderr + '\'' +
                ", exitStatus=" + exitStatus +
                '}';
    }
}