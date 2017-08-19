package com.mercury.platform.ui.frame.movable.container;

import com.mercury.platform.core.ProdStarter;
import com.mercury.platform.shared.FrameVisibleState;
import com.mercury.platform.shared.config.Configuration;
import com.mercury.platform.shared.config.configration.PlainConfigurationService;
import com.mercury.platform.shared.config.descriptor.NotificationSettingsDescriptor;
import com.mercury.platform.shared.entity.message.FlowDirections;
import com.mercury.platform.shared.store.MercuryStoreCore;
import com.mercury.platform.ui.components.ComponentsFactory;
import com.mercury.platform.ui.components.fields.font.FontStyle;
import com.mercury.platform.ui.components.fields.font.TextAlignment;
import com.mercury.platform.ui.components.panel.notification.NotificationPanel;
import com.mercury.platform.ui.components.panel.notification.ScannerNotificationPanel;
import com.mercury.platform.ui.components.panel.notification.factory.NotificationPanelFactory;
import com.mercury.platform.ui.frame.movable.AbstractMovableComponentFrame;
import com.mercury.platform.ui.misc.AppThemeColor;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;


public class NotificationFrame extends AbstractMovableComponentFrame {
    private List<NotificationPanel> notificationPanels;
    private PlainConfigurationService<NotificationSettingsDescriptor> config;
    private NotificationPanelFactory factory;
    private JPanel container;
    private JPanel expandPanel;
    private boolean expanded;
    @Override
    protected void initialize() {
        super.initialize();
        this.processSEResize = false;
        this.notificationPanels = new ArrayList<>();
        this.config = Configuration.get().notificationConfiguration();
        this.componentsFactory.setScale(this.scaleConfig.get("notification"));
        this.stubComponentsFactory.setScale(this.scaleConfig.get("notification"));
        this.factory = new NotificationPanelFactory();
    }

    @Override
    public void onViewInit() {
        this.getRootPane().setBorder(null);
        this.setBackground(AppThemeColor.TRANSPARENT);
        this.container = new JPanel();
        this.container.setBackground(AppThemeColor.TRANSPARENT);
        this.container.setLayout(new BoxLayout(container,BoxLayout.Y_AXIS));
        this.expandPanel = this.getExpandPanel();
        this.expandPanel.setVisible(false);
        this.add(this.expandPanel,BorderLayout.LINE_START);
        this.add(this.container,BorderLayout.CENTER);
        this.setVisible(true);
        this.pack();
    }

    @Override
    @SuppressWarnings("all")
    public void subscribe() {
        MercuryStoreCore.newNotificationSubject.subscribe(notification -> {
            SwingUtilities.invokeLater(() -> {
                NotificationPanel notificationPanel = this.factory.getProviderFor(notification.getType())
                        .setData(notification)
                        .setComponentsFactory(this.componentsFactory)
                        .build();
                this.notificationPanels.add(notificationPanel);
                this.container.add(notificationPanel);
                if(this.notificationPanels.size() > this.config.get().getLimitCount()){
                    if(!this.expanded) {
                        notificationPanel.setPaintAlphaValue(1f);
                        notificationPanel.setVisible(false);
                    }
                    this.expandPanel.setVisible(true);
                }
                this.pack();
                this.repaint();
                if(this.config.get().getFlowDirections().equals(FlowDirections.UPWARDS)
                        && !(notificationPanel instanceof ScannerNotificationPanel)){
                    this.setLocation(new Point(this.getLocation().x,this.getLocation().y - notificationPanel.getSize().height));
                }
                if(notificationPanel instanceof ScannerNotificationPanel){
                    Timer packTimer = new Timer(5, action -> {
                        this.pack();
                        if(this.config.get().getFlowDirections().equals(FlowDirections.UPWARDS)){
                            this.setLocation(new Point(this.getLocation().x,this.getLocation().y - notificationPanel.getSize().height));
                        }
                    });
                    packTimer.setRepeats(false);
                    packTimer.start();
                }
            });
        });
        MercuryStoreCore.removeNotificationSubject.subscribe(notification -> {
            SwingUtilities.invokeLater(() -> {
                NotificationPanel notificationPanel = this.notificationPanels.stream()
                        .filter(it -> it.getData().equals(notification))
                        .findAny().orElse(null);
                notificationPanel.onViewDestroy();
                int limitCount = this.config.get().getLimitCount();
                if(!this.expanded && this.notificationPanels.size() > limitCount){
                    this.notificationPanels.get(limitCount).setVisible(true);
                }
                this.container.remove(notificationPanel);
                this.notificationPanels.remove(notificationPanel);
                if(this.notificationPanels.size() - 1 < this.config.get().getLimitCount()){
                    this.expandPanel.setVisible(false);
                }
                this.pack();
                this.repaint();
                if(this.config.get().getFlowDirections().equals(FlowDirections.UPWARDS)
                        && this.notificationPanels.size() == 0){
                    this.setLocation(this.framesConfig.get("NotificationFrame").getFrameLocation());
                }
            });
        });
        MercuryStoreCore.hotKeySubject.subscribe(hotkeyDescriptor -> {
            SwingUtilities.invokeLater(() -> {
                if(this.notificationPanels.size() > 0 && ProdStarter.APP_STATUS.equals(FrameVisibleState.SHOW)){
                    this.notificationPanels.get(0).onHotKeyPressed(hotkeyDescriptor);
                }
            });
        });
    }

    @Override
    protected JPanel getPanelForPINSettings() {
        JPanel panel = this.componentsFactory.getJPanel(new BorderLayout(),AppThemeColor.FRAME);
        JLabel textLabel = this.componentsFactory.getTextLabel(FontStyle.BOLD, AppThemeColor.TEXT_DEFAULT, TextAlignment.CENTER, 22f, "Notification panel");
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(textLabel);
        panel.setPreferredSize(new Dimension((int)(400 * componentsFactory.getScale()), (int)(130*componentsFactory.getScale())));
        return panel;
    }

    @Override
    protected void registerDirectScaleHandler() {
    }

    @Override
    protected void performScaling(Map<String, Float> scaleData) {
    }
    @Override
    protected JPanel defaultView(ComponentsFactory factory) {
        return null;
    }

    @Override
    protected LayoutManager getFrameLayout() {
        return new BorderLayout();
    }

    private JPanel getExpandPanel(){
        JPanel root = this.componentsFactory.getJPanel(new BorderLayout());
        root.setBackground(AppThemeColor.MSG_HEADER);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1,1,1,0,AppThemeColor.FRAME),
                BorderFactory.createMatteBorder(1,1,1,1,AppThemeColor.RESPONSE_BUTTON_BORDER)));
        String iconPath = "app/collapse-all.png";
        JButton expandButton = componentsFactory.getIconButton(iconPath,22,AppThemeColor.MSG_HEADER,"");
        expandButton.addActionListener(action -> {
            if(this.expanded) {
                expandButton.setIcon(this.componentsFactory.getIcon("app/collapse-all.png",22));
                this.notificationPanels
                        .stream()
                        .skip(this.config.get().getLimitCount())
                        .forEach(it -> it.setVisible(false));
            }else {
                expandButton.setIcon(this.componentsFactory.getIcon("app/expand-all.png",22));
                this.notificationPanels.forEach(it -> {
                    if (!it.isVisible()) {
                        it.setVisible(true);
                    }
                });
            }
            this.expanded = !this.expanded;
            this.pack();
            this.repaint();
        });
        expandButton.setAlignmentY(SwingConstants.CENTER);
        root.add(expandButton,BorderLayout.CENTER);
        return root;
    }
}
