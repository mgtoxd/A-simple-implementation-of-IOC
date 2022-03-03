package pers.mtx.handWriteIOC.service.impl;

import pers.mtx.handWriteIOC.annonation.Service;
import pers.mtx.handWriteIOC.service.TestService;

@Service
public class TestServiceImpl implements TestService {
    @Override
    public String test(String name) {
        return name + "handWrite";
    }
}
