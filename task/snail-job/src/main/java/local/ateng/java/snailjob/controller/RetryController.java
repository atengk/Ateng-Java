package local.ateng.java.snailjob.controller;

import local.ateng.java.snailjob.service.RetryExecutorService;
import local.ateng.java.snailjob.service.RetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/retry")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RetryController {
    private final RetryService retryService;
    private final RetryExecutorService retryExecutorService;

    @GetMapping("/localRetry")
    public String localRetryApi() {
        retryService.localRetry();
        return "ok";
    }

    @GetMapping("/remoteRetry")
    public String remoteRetryApi() {
        retryService.remoteRetry();
        return "ok";
    }

    @GetMapping("/localRemoteRetry")
    public String localRemoteRetryApi() {
        retryService.localRemoteRetry();
        return "ok";
    }

    @GetMapping("/retryExecutor")
    public String retryExecutor() {
        retryExecutorService.myExecutorMethod();
        return "ok";
    }

}
