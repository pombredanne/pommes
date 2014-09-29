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
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.mount.Fstab;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.file.FileNode;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;

import java.io.IOException;
import java.util.List;

/**
 * Lists all indexed POMs in index *without* updating from the server. Warning: this will be a long list, only useful with grep.
 */
public class Find extends SearchBase<Pom> {
    private final Fstab fstab;

    public Find(Console console, Maven maven) throws IOException {
        super(console, maven);
        this.fstab = Fstab.load(console.world);
    }

    @Value(name = "query", position = 1)
    private String query;

    @Option("format")
    private String format = "%c @ %o %d";

    public List<Pom> search(Database database) throws IOException, QueryNodeException {
        return database.query(Fstab.load(console.world), query);
    }

    @Override
    public Pom toPom(Pom pom) {
        return pom;
    }

    @Override
    public String toLine(Pom pom) {
        return format(pom);
    }

    private String format(Pom pom) {
        char c;
        StringBuilder result;
        String url;
        boolean first;

        result = new StringBuilder();
        for (int i = 0, max = format.length(); i < max; i++) {
            c = format.charAt(i);
            if (c == '%' && i + 1 < max) {
                i++;
                c = format.charAt(i);
                switch (c) {
                    case '%':
                        result.append(c);
                        break;
                    case 'c':
                        result.append(pom.coordinates.toGavString());
                        break;
                    case 'g':
                        result.append(pom.coordinates.groupId);
                        break;
                    case 'a':
                        result.append(pom.coordinates.artifactId);
                        break;
                    case 'v':
                        result.append(pom.coordinates.version);
                        break;
                    case 'o':
                        result.append(pom.origin);
                        break;
                    case 'd':
                        first = true;
                        url = pom.projectUrl();
                        for (FileNode directory : fstab.directories(url)) {
                            if (directory.exists()) {
                                if (first) {
                                    first = false;
                                } else {
                                    result.append(' ');
                                }
                                result.append(directory.getAbsolute());
                            }
                        }
                        break;
                    default:
                        throw new IllegalStateException("invalid format character: " + c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
