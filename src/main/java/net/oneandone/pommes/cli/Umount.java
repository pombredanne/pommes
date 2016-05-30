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

import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.mount.Action;
import net.oneandone.pommes.mount.Fstab;
import net.oneandone.pommes.mount.Point;
import net.oneandone.pommes.mount.Remove;
import net.oneandone.pommes.mount.StatusException;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Umount extends Base {
    private final boolean stale;
    private final FileNode root;

    public Umount(Environment environment, boolean stale, FileNode root) {
        super(environment);
        this.stale = stale;
        this.root = root;
    }

    @Override
    public void run(Database database) throws Exception {
        int problems;
        Fstab fstab;
        List<FileNode> checkouts;
        Pom scannedPom;
        List<Action> removes;
        Point point;
        FileNode configuredDirectory;

        fstab = Fstab.load(world);
        checkouts = new ArrayList<>();
        Scm.scanCheckouts(root, checkouts);
        if (checkouts.isEmpty()) {
            throw new ArgumentException("no checkouts under " + root);
        }
        removes = new ArrayList<>();
        problems = 0;
        for (FileNode directory : checkouts) {
            if (stale && !isStale(database, directory)) {
                continue;
            }
            scannedPom = environment.scanPom(directory);
            point = fstab.pointOpt(directory);
            if (point == null) {
                console.error.println("? " + directory + " (" + scannedPom + ")");
                problems++;
            } else {
                configuredDirectory = point.directory(scannedPom);
                if (directory.equals(configuredDirectory)) {
                    try {
                        removes.add(Remove.create(directory, scannedPom));
                    } catch (StatusException e) {
                        console.error.println(e.getMessage());
                        problems++;
                    }
                } else {
                    console.error.println("C " + directory + " vs " + configuredDirectory + " (" + scannedPom + ")");
                    problems++;
                }
            }
        }
        if (problems > 0) {
            throw new IOException("aborted - fix the above problem(s) first: " + problems);
        }
        runAll(removes);
    }

    private boolean isStale(Database database, FileNode directory) throws IOException {
        return false;
        /* TODO
        return database.lookup("svn:" + Base.scanUrl(directory) + "pom.xml") == null
                && database.lookup("svn:" + Base.scanUrl(directory) + "composer.json") == null;
                */
    }
}
