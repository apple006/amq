package com.artlongs.amq.core.admin;

import com.artlongs.amq.http.Render;
import com.artlongs.amq.http.routes.Controller;
import com.artlongs.amq.http.routes.Get;

import java.util.ArrayList;
import java.util.List;

/**
 * Func :
 *
 * @author: leeton on 2019/3/19.
 */
public abstract class BaseController {

    private List<Controller> controllerList = new ArrayList<>();

    public BaseController() {
        set(asset);
    }

    public Controller[] getControllers() {
        addController();
        return controllerList.toArray(new Controller[controllerList.size()]);
    }

    protected abstract void addController();

    public BaseController set(Controller controller) {
        this.controllerList.add(controller);
        return this;
    }

    Controller asset = new Controller() {
        @Get("/views/asset/{all}")
        public Render topicIndex(String all) {
            return Render.template("/asset/" + all);
        }
    };


}
