package local.ateng.java.jetcache.remote;

import com.alicp.jetcache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/remote")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RemoteController {

    private final RemoteService remoteService;
    private final Cache<Integer, String> manualRemoteCache;

    @GetMapping("/data/{id}")
    public String getData(@PathVariable int id) {
        return remoteService.getDataById(id);
    }

    @GetMapping("/update/{id}/{newData}")
    public String updateData(@PathVariable int id, @PathVariable String newData) {
        return remoteService.updateData(id, newData);
    }

    @GetMapping("/delete/{id}")
    public void deleteData(@PathVariable int id) {
        remoteService.deleteData(id);
    }

    @GetMapping("/manual/put/{id}")
    public void manualPut(@PathVariable int id) {
        manualRemoteCache.put(id, "Hello World");
    }

    @GetMapping("/manual/get/{id}")
    public String manualGet(@PathVariable int id) {
        return manualRemoteCache.get(id);
    }

    @GetMapping("/manual/list")
    public Map<Integer, String> manualList() {
        return manualRemoteCache.getAll(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

}
