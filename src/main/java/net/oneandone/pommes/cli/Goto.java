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

import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Field;
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.model.PommesQuery;
import net.oneandone.pommes.mount.Action;
import net.oneandone.pommes.mount.Checkout;
import net.oneandone.pommes.mount.Nop;
import net.oneandone.setenv.Setenv;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Goto extends Base {
    private final List<String> query;

    public Goto(Environment environment, List<String> query) {
        super(environment);
        this.query = query;
    }

    @Override
    public void run(Database database) throws Exception {
        Setenv setenv;
        List<Action> actions;
        FileNode directory;
        Action action;

        actions = new ArrayList<>();
        for (Pom pom : Field.poms(database.query(PommesQuery.create(query, environment)))) {
            directory = environment.properties().root.directory(pom);
            action = Checkout.createOpt(directory, pom);
            if (action == null) {
                action = new Nop(directory);
            }
            if (!actions.contains(action)) {
                actions.add(action);
            }
        }
        action = runSingle(actions);
        if (action == null) {
            throw new IOException("nothing selected");
        }
        setenv = Setenv.get();
        if (setenv.isConfigured()) {
            setenv.cd(action.directory.getAbsolute());
        } else {
            console.info.println("cd " + action.directory.getAbsolute());
        }
    }

    /** @return never null */
    protected Action runSingle(Collection<Action> actionsOrig) throws Exception {
        List<Action> actions;
        int no;
        String selection;
        Action last;

        if (actionsOrig.isEmpty()) {
            throw new IOException("not found: " + query);
        }
        actions = new ArrayList<>(actionsOrig);
        if (actions.size() == 1) {
            last = actions.get(0);
            last.run(console);
            return last;
        }
        last = null;
        do {
            no = 1;
            for (Action action : actions) {
                console.info.println("[" + no + "] " + action);
                no++;
            }
            selection = console.readline("Please select: ");
            try {
                no = Integer.parseInt(selection);
                if (no > 0 && no <= actions.size()) {
                    last = actions.remove(no - 1);
                    last.run(console);
                } else {
                    console.info.println("action not found: " + no);
                }
            } catch (NumberFormatException e) {
                console.info.println("action not found: " + no);
            }
        } while (last == null);
        return last;
    }
}
