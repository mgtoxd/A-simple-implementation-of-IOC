package pers.mtx.handWriteIOC.controller;

import pers.mtx.handWriteIOC.annonation.Autowired;
import pers.mtx.handWriteIOC.annonation.Controller;
import pers.mtx.handWriteIOC.annonation.RequestMapping;
import pers.mtx.handWriteIOC.service.TestService;

@Controller
@RequestMapping("/handWrite")
public class TestController {
    @Autowired
    private TestService testService;
    @RequestMapping("test")
    public String test(){
        return testService.test("wdadwa");
    }
}
