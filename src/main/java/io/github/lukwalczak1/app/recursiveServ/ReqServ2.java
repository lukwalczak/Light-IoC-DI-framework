package io.github.lukwalczak1.app.recursiveServ;

import io.github.lukwalczak1.framework.container.annotations.beans.Service;
import io.github.lukwalczak1.framework.container.annotations.injection.Inject;

@Service
public class ReqServ2 {

    private final ReqServ1 reqServ1;

    public ReqServ2() {
        this.reqServ1 = null;
            System.out.println("reqServ2 created");
    }

    @Inject
    public ReqServ2(ReqServ1 reqServ1) {
        this.reqServ1 = reqServ1;
        System.out.println("ReqServ2 created with ReqServ1");
    }
}
