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
import net.oneandone.pommes.model.GAV;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Find extends Base {
    private final String query;
    private final String format;

    public Find(Globals globals, String query, String format) throws IOException {
        super(globals);
        this.query = query;
        this.format = format;
    }


    @Override
    public void run(Database database) throws Exception {
        List<Pom> matches;

        matches = database.query(query, environment);
        for (Pom pom : matches) {
            console.info.println(format(pom));
        }
    }

    private String format(Pom pom) throws IOException {
        char c;
        StringBuilder result;
        String url;
        int end;
        String variable;
        String filter;
        List<String> values;
        boolean first;

        result = new StringBuilder();
        for (int i = 0, max = format.length(); i < max; i++) {
            c = format.charAt(i);
            if (c == '%' && i + 1 < max) {
                i++;
                c = format.charAt(i);
                values = new ArrayList<>();
                switch (c) {
                    case '%':
                        result.append(c);
                        values.add(Character.toString(c));
                        break;
                    case 'g':
                        values.add(pom.coordinates.toGavString());
                        break;
                    case 'o':
                        values.add(pom.origin);
                        break;
                    case 'd':
                        for (GAV d : pom.dependencies) {
                            values.add(d.toGavString());
                        }
                        break;
                    case 'c':
                        url = pom.projectUrl();
                        for (FileNode directory : environment.fstab().directories(url)) {
                            if (directory.exists()) {
                                values.add(directory.getAbsolute());
                            }
                        }
                        break;
                    default:
                        throw new IllegalStateException("invalid format character: " + c);
                }
                if (i + 1 < max && format.charAt(i + 1) == '[') {
                    end = format.indexOf(']', i + 2);
                    if (end == -1) {
                        throw new IllegalStateException("invalid format: " + format);
                    }
                    filter = format.substring(i + 2, end);
                    if (filter.startsWith("=")) {
                        if (filter.length() == 1 || !filter.endsWith("=")) {
                            throw new IllegalStateException("variable is not terminated: " + filter);
                        }
                        variable = filter.substring(1, filter.length() - 1);
                        filter = environment.lookup(variable);
                        if (filter == null) {
                            throw new IllegalStateException("unknown variable in format: " + variable);
                        }
                    }
                    i = end;
                } else {
                    filter = "";
                }
                first = true;
                for (String value : values) {
                    if (value.contains(filter)) {
                        if (first) {
                            first = false;
                        } else {
                            result.append(' ');
                        }
                        result.append(value);
                    }
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
