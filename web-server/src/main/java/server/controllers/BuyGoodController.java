package server.controllers;

import org.springframework.web.bind.annotation.*;
import server.msgtypes.YesNo;

@RestController
public class BuyGoodController {

    @RequestMapping(value = "/buygood/{buyerid}/{goodid}", method = RequestMethod.PUT)
    public YesNo buyGood(@PathVariable("buyerid") long buyerId, @PathVariable("goodid") long goodId) {
        System.out.println(String.format("PUT Request @ /buygood -> buyerId: %d, goodId: %d", buyerId, goodId));
        return new YesNo(false);
    }
}