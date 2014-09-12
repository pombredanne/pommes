/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.pommes.cli;

import net.oneandone.maven.embedded.Maven;
import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.mount.Action;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Command;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Separator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class Base implements Command {
    @Option("local")
    private boolean local;

    @Option("global")
    private boolean global;

    protected final Console console;
    protected final Maven maven;

    public Base(Console console, Maven maven) {
        this.console = console;
        this.maven = maven;
    }

    public void invoke() throws Exception {
        Node node;

        if (local && global) {
            throw new ArgumentException("cannot combine local and global mode");
        }
        try (Database database = Database.load(console.world)) {
            if (global) {
                database.update();
            } else if (!local) {
                database.updateOpt();
            }
            invoke(database);
            if (global) {
                node = database.upload();
                console.info.println("uploaded global pommes database: " + node.getURI() + ", " + (node.length() / 1024) + "k");
            }
        }
    }

    public abstract void invoke(Database database) throws Exception;

    protected void runAll(Collection<Action> actionsOrig) throws Exception {
        List<Action> actions;
        int no;
        int problems;
        String selection;

        actions = new ArrayList<>(actionsOrig);
        if (actions.isEmpty()) {
            console.info.println("nothing to do");
            return;
        }
        Collections.sort(actions);
        do {
            no = 1;
            for (Action action : actions) {
                console.info.println("[" + no + "] " + action);
                no++;
            }
            console.info.println("[d]      done without actions");
            console.info.println("[return] all");
            selection = console.readline("Selection(s): ");
            if (selection.isEmpty()) {
                problems = 0;
                for (Action action : actions) {
                    try {
                        action.run(console);
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        console.error.println(e.getMessage());
                        problems++;
                    }
                }
                actions.clear();
                if (problems > 0) {
                    throw new IOException(problems + " actions failed");
                }
            } else for (String one : Separator.SPACE.split(selection)) {
                if (one.equals("d")) {
                    console.info.println("done");
                    actions.clear();
                } else {
                    try {
                        no = Integer.parseInt(one);
                        if (no > 0 && no <= actions.size()) {
                            actions.remove(no - 1).run(console);
                        } else {
                            console.info.println("action not found: " + one);
                        }
                    } catch (NumberFormatException e) {
                        console.info.println("action not found: " + one);
                    }
                }
            }
        } while (!actions.isEmpty());
    }

    //--

    public static Launcher svn(FileNode dir, String ... args) {
        Launcher launcher;

        launcher = new Launcher(dir);
        launcher.arg("svn");
        launcher.arg("--non-interactive");
        launcher.arg("--trust-server-cert"); // needs svn >= 1.6
        launcher.arg(args);
        return launcher;
    }

    public static boolean notCommitted(FileNode directory) throws IOException {
        return notCommitted(directory.exec("svn", "status"));
    }

    private static boolean notCommitted(String lines) {
        for (String line : Separator.on("\n").split(lines)) {
            if (line.trim().length() > 0) {
                if (line.startsWith("X") || line.startsWith("Performing status on external item")) {
                    // needed for pws workspace svn:externals  -> ok
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public static String scanUrl(FileNode directory) throws IOException {
        String result;

        result = scanUrlOpt(directory);
        if (result == null) {
            throw new IllegalStateException(directory.toString());
        }
        return result;
    }

    /** @return null if not a working copy */
    public static String scanUrlOpt(FileNode directory) throws IOException {
        String url;
        int idx;

        if (!directory.join(".svn").exists()) {
            return null;
        }
        url = directory.launcher("svn", "info").exec();
        idx = url.indexOf("URL: ") + 5;
        return Database.withSlash(url.substring(idx, url.indexOf("\n", idx)));
    }

    //--

    public void scanCheckouts(FileNode directory, List<FileNode> result) throws IOException {
        List<FileNode> children;

        if (directory.join(".svn").isDirectory()) {
            result.add(directory);
        } else {
            children = directory.list();
            if (children != null) {
                for (FileNode child : children) {
                    scanCheckouts(child, result);
                }
            }
        }
    }
}
