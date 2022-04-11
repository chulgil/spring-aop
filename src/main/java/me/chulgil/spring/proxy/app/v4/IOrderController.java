package me.chulgil.spring.proxy.app.v4;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping
@ResponseBody
public interface IOrderController {

    @GetMapping("/v1/request")
    String request(@RequestParam("itemId") String itemIdo);

    @GetMapping("/v1/no-log")
    String noLog();

}
