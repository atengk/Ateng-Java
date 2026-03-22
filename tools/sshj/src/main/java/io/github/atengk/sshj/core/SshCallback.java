package io.github.atengk.sshj.core;

import net.schmizz.sshj.SSHClient;

@FunctionalInterface
public interface SshCallback<T> {

    T doInSsh(SSHClient sshClient) throws Exception;
}