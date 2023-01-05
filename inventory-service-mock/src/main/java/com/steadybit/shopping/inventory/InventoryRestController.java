package com.steadybit.shopping.inventory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryRestController {

    @GetMapping
    public boolean isAvailable(@RequestParam(value = "id") String id) {
        return Math.random() > 0.005;
    }

}
