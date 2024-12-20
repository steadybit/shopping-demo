package com.steadybit.shopping.inventory;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryRestController {

    @GetMapping
    public boolean isAvailable(@RequestParam String id) {
        return ThreadLocalRandom.current().nextDouble() > 0.005;
    }

}
