package local.ateng.java.jetcache.local;

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
@RequestMapping("/local")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LocalController {

    private final LocalService localService;
    private final Cache<Integer, String> manualCache;

    @GetMapping("/data/{id}")
    public String getData(@PathVariable int id) {
        return localService.getDataById(id);
    }

    @GetMapping("/update/{id}/{newData}")
    public String updateData(@PathVariable int id, @PathVariable String newData) {
        return localService.updateData(id, newData);
    }

    @GetMapping("/delete/{id}")
    public void deleteData(@PathVariable int id) {
        localService.deleteData(id);
    }

    @GetMapping("/manual/put/{id}")
    public void manualPut(@PathVariable int id) {
        manualCache.put(id, "Hello World");
    }

    @GetMapping("/manual/get/{id}")
    public String manualGet(@PathVariable int id) {
        return manualCache.get(id);
    }

    @GetMapping("/manual/list")
    public Map<Integer, String> manualList() {
        return manualCache.getAll(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

}
