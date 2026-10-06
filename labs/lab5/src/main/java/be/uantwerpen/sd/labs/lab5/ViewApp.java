package be.uantwerpen.sd.labs.lab5;

import be.uantwerpen.sd.labs.lab5.controller.Controller;
import be.uantwerpen.sd.labs.lab5.controller.RegistrationController;
import be.uantwerpen.sd.labs.lab5.database.Database;
import be.uantwerpen.sd.labs.lab5.database.RegistrationDB;
import be.uantwerpen.sd.labs.lab5.view.View;
import be.uantwerpen.sd.labs.lab5.viewfx.RegistrationView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ViewApp extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Database model = new RegistrationDB();

        Controller controller = new RegistrationController(model);

        RegistrationView view = new RegistrationView();

        View viewLogic = new View(model, controller, view);

        view.attachLogic(viewLogic);

        Scene scene = new Scene(view, 960, 560);
        stage.setTitle("Lab 5 — GoF MVC");
        stage.setScene(scene);
        stage.show();
    }

}
