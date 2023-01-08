package com.example.application.views.setting;

import com.example.application.data.entity.Setting;
import com.example.application.data.service.SettingService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@PageTitle("Setting")
@Route(value = "setting/:settingID?/:action?(edit)", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class SettingView extends Div implements BeforeEnterObserver {

    private final String SETTING_ID = "settingID";
    private final String SETTING_EDIT_ROUTE_TEMPLATE = "setting/%s/edit";

    private final Grid<Setting> grid = new Grid<>(Setting.class, false);

    private Upload gambar;
    private Image gambarPreview;
    private TextField tentang;
    private TextField namaDokter;
    private TextField lokasi;
    private TextField noTelpon;
    private TextField email;

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final BeanValidationBinder<Setting> binder;

    private Setting setting;

    private final SettingService settingService;

    public SettingView(SettingService settingService) {
        this.settingService = settingService;
        addClassNames("setting-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        LitRenderer<Setting> gambarRenderer = LitRenderer.<Setting>of(
                "<span style='border-radius: 50%; overflow: hidden; display: flex; align-items: center; justify-content: center; width: 64px; height: 64px'><img style='max-width: 100%' src=${item.gambar} /></span>")
                .withProperty("gambar", item -> {
                    if (item != null && item.getGambar() != null) {
                        return "data:image;base64," + Base64.getEncoder().encodeToString(item.getGambar());
                    } else {
                        return "";
                    }
                });
        grid.addColumn(gambarRenderer).setHeader("Gambar").setWidth("96px").setFlexGrow(0);

        grid.addColumn("tentang").setAutoWidth(true);
        grid.addColumn("namaDokter").setAutoWidth(true);
        grid.addColumn("lokasi").setAutoWidth(true);
        grid.addColumn("noTelpon").setAutoWidth(true);
        grid.addColumn("email").setAutoWidth(true);
        grid.setItems(query -> settingService.list(
                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)))
                .stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(SETTING_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(SettingView.class);
            }
        });

        // Configure Form
        binder = new BeanValidationBinder<>(Setting.class);

        // Bind fields. This is where you'd define e.g. validation rules

        binder.bindInstanceFields(this);

        attachImageUpload(gambar, gambarPreview);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.setting == null) {
                    this.setting = new Setting();
                }
                binder.writeBean(this.setting);
                settingService.update(this.setting);
                clearForm();
                refreshGrid();
                Notification.show("Data updated");
                UI.getCurrent().navigate(SettingView.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                Notification n = Notification.show(
                        "Error updating the data. Somebody else has updated the record while you were making changes.");
                n.setPosition(Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (ValidationException validationException) {
                Notification.show("Failed to update the data. Check again that all values are valid");
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> settingId = event.getRouteParameters().get(SETTING_ID).map(Long::parseLong);
        if (settingId.isPresent()) {
            Optional<Setting> settingFromBackend = settingService.get(settingId.get());
            if (settingFromBackend.isPresent()) {
                populateForm(settingFromBackend.get());
            } else {
                Notification.show(String.format("The requested setting was not found, ID = %s", settingId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(SettingView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        Label gambarLabel = new Label("Gambar");
        gambarPreview = new Image();
        gambarPreview.setWidth("100%");
        gambar = new Upload();
        gambar.getStyle().set("box-sizing", "border-box");
        gambar.getElement().appendChild(gambarPreview.getElement());
        tentang = new TextField("Tentang");
        namaDokter = new TextField("Nama Dokter");
        lokasi = new TextField("Lokasi");
        noTelpon = new TextField("No Telpon");
        email = new TextField("Email");
        formLayout.add(gambarLabel, gambar, tentang, namaDokter, lokasi, noTelpon, email);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void attachImageUpload(Upload upload, Image preview) {
        ByteArrayOutputStream uploadBuffer = new ByteArrayOutputStream();
        upload.setAcceptedFileTypes("image/*");
        upload.setReceiver((fileName, mimeType) -> {
            uploadBuffer.reset();
            return uploadBuffer;
        });
        upload.addSucceededListener(e -> {
            StreamResource resource = new StreamResource(e.getFileName(),
                    () -> new ByteArrayInputStream(uploadBuffer.toByteArray()));
            preview.setSrc(resource);
            preview.setVisible(true);
            if (this.setting == null) {
                this.setting = new Setting();
            }
            this.setting.setGambar(uploadBuffer.toByteArray());
        });
        preview.setVisible(false);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(Setting value) {
        this.setting = value;
        binder.readBean(this.setting);
        this.gambarPreview.setVisible(value != null);
        if (value == null || value.getGambar() == null) {
            this.gambar.clearFileList();
            this.gambarPreview.setSrc("");
        } else {
            this.gambarPreview.setSrc("data:image;base64," + Base64.getEncoder().encodeToString(value.getGambar()));
        }

    }
}
