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
package net.oneandone.pommes.source;

import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.cli.Item;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.filter.Filter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class NodeSource implements Source {
    public static final String FILE = "file:";
    public static final String SVN = "svn:";

    public static NodeSource createOpt(World world, String url) throws URISyntaxException, NodeInstantiationException {
        if (url.startsWith(SVN) || url.startsWith(FILE)) {
            return new NodeSource(world.node(url));
        } else {
            return null;
        }
    }

    //--

    private final Node node;
    private boolean withBranches;
    private final Filter exclude;

    public NodeSource(Node node) {
        this.node = node;
        this.exclude = new Filter();
        this.withBranches = true;
    }

    @Override
    public void addOption(String option) {
        if (option.equals("NO_BRANCHES")) {
            withBranches = false;
        }
    }

    @Override
    public void addExclude(String str) {
        if (str.startsWith("/") || str.endsWith("/")) {
            throw new ArgumentException("do not use '/' before or after excludes: " + str);
        }
        exclude.include("**/" + str);
    }

    @Override
    public void scan(BlockingQueue<Item> dest) throws IOException, InterruptedException {
        scan(node, true, dest);
    }

    public void scan(Node root, boolean recurse, BlockingQueue<Item> dest) throws IOException, InterruptedException {
        List<? extends Node> children;
        Node project;
        Node trunk;
        Node branches;
        List<? extends Node> grandChildren;

        if (exclude.matches(root.getPath())) {
            return;
        }
        children = root.list();
        if (children == null) {
            return;
        }
        project = pom(children);
        if (project != null) {
            dest.put(new Item(project));
            return;
        }
        project = child(children, "composer.json");
        if (project != null) {
            dest.put(new Item(node));
            return;
        }
        trunk = child(children, "trunk");
        if (trunk != null) {
            scan(trunk, false, dest);
        }
        branches = child(children, "branches");
        if (branches != null && withBranches) {
            grandChildren = branches.list();
            if (grandChildren != null) {
                for (Node grandChild : grandChildren) {
                    scan(grandChild, false, dest);
                }
            }
        }
        if (trunk != null || branches != null) {
            return;
        }

        if (child(children, "src") != null) {
            // probably an old ant build
            return;
        }

        if (recurse) {
            // recurse
            for (Node node : children) {
                scan(node, true, dest);
            }
        }
    }

    private static Node child(List<? extends Node> childen, String name) {
        for (Node node : childen) {
            if (node.getName().equals(name)) {
                return node;
            }
        }
        return null;
    }

    private static Node pom(List<? extends Node> childen) {
        String name;

        for (Node node : childen) {
            name = node.getName();
            if (name.equals("pom.xml") || name.endsWith(".pom")) {
                return node;
            }
        }
        return null;
    }
}
