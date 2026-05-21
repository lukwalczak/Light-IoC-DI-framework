package io.github.lukwalczak1.app.recursiveServ;

import io.github.lukwalczak1.framework.container.annotations.beans.Service;
import io.github.lukwalczak1.framework.container.annotations.injection.Inject;
import io.github.lukwalczak1.framework.scope.annotation.Lazy;

@Lazy
@Service
public class ReqServ1 {

    private final ReqServ2 reqServ2;

    public ReqServ1() {
        this.reqServ2 = null;
        System.out.println("reqServ1 created");
    }

    @Inject
    public ReqServ1(ReqServ2 reqServ2) {
        System.out.println("ReqServ1 created with ReqServ2");
        this.reqServ2 = reqServ2;
    }
}
