/*  MUD Map (v2) - A tool to create and organize maps for text-based games
 *  Copyright (C) 2014  Neop (email: mneop@web.de)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, see <http://www.gnu.org/licenses/>.
 */

/*  File description
 *
 *  This class constructs the main window and the available worlds tab. It also
 *  reads and writes the main config file
 */

package mudmap2.frontend;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mudmap2.Paths;
import mudmap2.backend.World;
import mudmap2.backend.WorldFileList;
import mudmap2.backend.WorldFileReader.current.WorldFileMM1;
import mudmap2.backend.WorldManager;
import mudmap2.frontend.dialog.AboutDialog;
import mudmap2.frontend.dialog.AreaDialog;
import mudmap2.frontend.dialog.EditWorldDialog;
import mudmap2.frontend.dialog.ExportImageDialog;
import mudmap2.frontend.dialog.OpenWorldDialog;
import mudmap2.frontend.dialog.PathColorDialog;
import mudmap2.frontend.dialog.PlaceListDialog;

/**
 * Main class for the mudmap window
 * @author neop
 */
public final class Mainwindow extends JFrame {

    static int config_file_version_major = 2;
    static int config_file_version_minor = 0;

    // Contains all opened maps <name, worldtab>
    HashMap<World, WorldTab> world_tabs;

    // GUI elements
    JCheckBoxMenuItem menu_edit_curved_paths, menu_edit_show_cursor;

    JTabbedPane tabbed_pane;
    AvailableWorldsTab available_worlds_tab;

    public Mainwindow(){
        super("MUD Map " + Mainwindow.class.getPackage().getImplementationVersion());

        // set program icon
        try {
            /*URL url_icon = ClassLoader.getSystemResource("resources/mudmap.svg");
            Image icon = Toolkit.getDefaultToolkit().createImage(url_icon);
            setIconImage(icon);*/
        } catch(Exception e){
            System.out.println(e);
        }

        setMinimumSize(new Dimension(400, 300));

        // create GUI
        world_tabs = new HashMap<>();

        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                quit();
            }
        });

        // Add GUI components
        JMenuBar menu_bar = new JMenuBar();
        add(menu_bar, BorderLayout.NORTH);

        JMenu menu_file = new JMenu("File");
        menu_bar.add(menu_file);
        JMenu menu_edit = new JMenu("World"); // Edit renamed to World
        menu_bar.add(menu_edit);
        JMenu menu_help = new JMenu("Help");
        menu_bar.add(menu_help);

        JMenuItem menu_file_new = new JMenuItem("New");
        menu_file.add(menu_file_new);
        menu_file_new.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                createNewWorld();
            }
        });

        JMenuItem menu_file_open = new JMenuItem("Open");
        menu_file.add(menu_file_open);
        menu_file_open.addActionListener(new OpenWorldDialog(this));

        menu_file.addSeparator();
        JMenuItem menu_file_save = new JMenuItem("Save");
        menu_file_save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        menu_file.add(menu_file_save);
        menu_file_save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                WorldTab wt = getSelectedTab();
                if(wt != null) wt.save();
            }
        });

        JMenuItem menu_file_save_as_image = new JMenuItem("Export as image");
        menu_file.add(menu_file_save_as_image);
        menu_file_save_as_image.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                WorldTab wt = getSelectedTab();
                if(wt != null){
                    ExportImageDialog dlg = new ExportImageDialog(wt.parent, wt);
                    dlg.setVisible(true);
                }
            }
        });

        menu_file.addSeparator();
        JMenuItem menu_file_quit = new JMenuItem("Quit");
        menu_file.add(menu_file_quit);
        menu_file_quit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                quit();
            }
        });

        JMenuItem menu_edit_edit_world = new JMenuItem("Edit world");
        menu_edit.add(menu_edit_edit_world);
        menu_edit_edit_world.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorldTab tab = getSelectedTab();
                if(tab != null){
                    (new EditWorldDialog(tab.parent, tab.getWorld())).setVisible(true);
                    available_worlds_tab.update();
                }
            }
        });

        JMenuItem menu_edit_path_colors = new JMenuItem("Path colors");
        menu_edit.add(menu_edit_path_colors);
        menu_edit_path_colors.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                WorldTab tab = getSelectedTab();
                if(tab != null){
                    (new PathColorDialog(tab.parent, tab.getWorld())).setVisible(true);
                    tab.repaint();
                }
            }
        });

        JMenuItem menu_edit_add_area = new JMenuItem("Add area");
        menu_edit.add(menu_edit_add_area);
        menu_edit_add_area.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                WorldTab wt = getSelectedTab();
                if(wt != null) (new AreaDialog(wt.parent, wt.getWorld())).setVisible(true);
            }
        });

        menu_edit.add(new JSeparator());

        JMenuItem menu_edit_set_home_position = new JMenuItem("Set home position");
        menu_edit.add(menu_edit_set_home_position);
        menu_edit_set_home_position.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                WorldTab wt = getSelectedTab();
                if(wt != null) wt.setHome();
            }
        });
        JMenuItem menu_edit_goto_home_position = new JMenuItem("Go to home position");
        menu_edit.add(menu_edit_goto_home_position);
        menu_edit_goto_home_position.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                WorldTab wt = getSelectedTab();
                if(wt != null) wt.gotoHome();
            }
        });

        menu_edit.add(new JSeparator());

        JMenuItem menu_edit_list_places = new JMenuItem("List places");
        menu_edit.add(menu_edit_list_places);
        menu_edit_list_places.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                WorldTab wt = getSelectedTab();
                // show place list
                if(wt != null) (new PlaceListDialog(wt, false)).setVisible(true);
            }

        });

        menu_edit.add(new JSeparator());

        menu_edit_curved_paths = new JCheckBoxMenuItem("Curved paths");
        menu_edit.add(menu_edit_curved_paths);
        // will be set after the config file is read
        //menu_edit_curved_paths.setSelected(WorldTab.getShowPathsCurved());
        menu_edit_curved_paths.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                WorldTab.setShowPathsCurved(((JCheckBoxMenuItem) e.getSource()).isSelected());
                WorldTab cur_tab = getSelectedTab();
                if(cur_tab != null) cur_tab.repaint();
            }
        });

        menu_edit_show_cursor = new JCheckBoxMenuItem("Show place cursor");
        menu_edit.add(menu_edit_show_cursor);
        menu_edit_show_cursor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));
        menu_edit_show_cursor.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                WorldTab cur_tab = getSelectedTab();
                if(cur_tab != null){
                    cur_tab.setCursorEnabled(((JCheckBoxMenuItem) e.getSource()).isSelected());
                    cur_tab.repaint();
                }
            }
        });

        JMenuItem menu_help_help = new JMenuItem("Help (online)");
        menu_help.add(menu_help_help);
        menu_help_help.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                Paths.openWebsite(Paths.manual_url);
            }
        });

        JMenuItem menu_help_about = new JMenuItem("About");
        menu_help.add(menu_help_about);
        menu_help_about.addActionListener((ActionListener) new AboutDialog(this));

        // ---
        readConfig();

        // ---
        tabbed_pane = new JTabbedPane();
        add(tabbed_pane);
        tabbed_pane.addTab("Available worlds", available_worlds_tab = new AvailableWorldsTab(this));
        tabbed_pane.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent ce) {
                WorldTab cur_tab = getSelectedTab();
                if(cur_tab != null) cur_tab.updateCursorEnabled();
            }
        });

        setVisible(true);
    }

    public void createNewWorld(){
        String name = JOptionPane.showInputDialog(this, "Enter new world name", "New world", JOptionPane.PLAIN_MESSAGE);
        if(name != null && !name.isEmpty()){
            // create a new world
            try {
                World world = WorldManager.createWorld(name);
                available_worlds_tab.update();
                createTab(world, null);
            } catch (Exception ex) {
                Logger.getLogger(Mainwindow.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(this, "Couldn't create world \"" + name + "\":\n" + ex.getMessage());
            }
        }
    }

    /**
     * create world tab
     * @param world
     * @param file world file or empty string / null
     */
    public void createTab(World world, String file){
        setMinimumSize(new Dimension(500, 400));

        if(!world_tabs.containsKey(world)){
            // open new tab
            WorldTab tab = new WorldTab(this, world, file, false);
            world_tabs.put(world, tab);
            tabbed_pane.addTab(tab.getWorld().getName(), tab);
        }
        // change current tab
        tabbed_pane.setSelectedComponent(world_tabs.get(world));

        WorldTab curTab = getSelectedTab();
        if(curTab != null){
            available_worlds_tab.update();

            // update menu entry
            menu_edit_show_cursor.setState(curTab.getCursorEnabled());
        }
    }

    public void updateCurTab(){

    }

    /**
     * Closes all tabs
     */
    public void closeTabs(){
        for(WorldTab tab: world_tabs.values()) tab.close();
    }

    /**
     * Removes a tab without saving and closing the world in WorldManager
     * @param tab
     */
    public void removeTab(WorldTab tab){
        tabbed_pane.remove(tab);
    }

    /**
     * Gets the currently shown WorldTab
     * @return WorldTab or null
     */
    private WorldTab getSelectedTab(){
        if(tabbed_pane != null){
            Component ret = tabbed_pane.getSelectedComponent();
            if(ret instanceof WorldTab) return (WorldTab) ret;
        }
        return null;
    }

    public JCheckBoxMenuItem getMiShowPlaceSelection(){
        return menu_edit_show_cursor;
    }

    /**
     * Saves all config
     */
    public void quit(){
        writeConfig();
        closeTabs();
        WorldFileList.writeWorldList();
        System.exit(0);
    }

    /**
     * Reads MUD Map config file
     */
    public void readConfig(){
        try {
            BufferedReader reader = new BufferedReader(new FileReader(mudmap2.Paths.getConfigFile()));

            String line;
            int file_major = 0, file_minor = 0;

            try {
                while((line = reader.readLine()) != null){
                    line = line.trim();

                    if(line.startsWith("ver")){
                        String[] tmp = line.substring(4).split("\\.");
                        file_major = Integer.parseInt(tmp[0]);
                        file_minor = Integer.parseInt(tmp[1]);
                    } else if(line.startsWith("show_paths_curved")){ // show curved path lines - if path lines are enabled
                        String[] tmp = line.split(" ");
                        WorldTab.setShowPathsCurved(Boolean.parseBoolean(tmp[1]));
                    } else if(line.startsWith("compat_mudmap_1")){ // save world files compatible to mudmap 1
                        String[] tmp = line.split(" ");
                        WorldFileMM1.compatibility_mudmap_1 = Boolean.parseBoolean(tmp[1]);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(WorldManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Couldn't open config file \"" + mudmap2.Paths.getConfigFile() + "\", file not found");
            //Logger.getLogger(WorldManager.class.getName()).log(Level.INFO, null, ex);
        }

        if(menu_edit_curved_paths != null) menu_edit_curved_paths.setSelected(WorldTab.getShowPathsCurved());
    }

    /**
     * Writes MUD Map config file
     */
    public void writeConfig(){
        // open file
        if(!Paths.isDirectory(Paths.getUserDataDir())) Paths.createDirectory(Paths.getUserDataDir());

        try (PrintWriter outstream = new PrintWriter(new BufferedWriter( new FileWriter(Paths.getConfigFile())))) {
            outstream.println("# MUD Map 2 config file");
            outstream.println("ver " + config_file_version_major + "." + config_file_version_minor);
            outstream.println("show_paths_curved " + WorldTab.getShowPathsCurved());
            outstream.println("compat_mudmap_1 " + WorldFileMM1.compatibility_mudmap_1);
        } catch (IOException ex) {
            System.out.printf("Couldn't write config file " + Paths.getConfigFile());
            Logger.getLogger(Mainwindow.class.getName()).log(Level.WARNING, null, ex);
        }
    }

}