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
package net.oneandone.pommes.model;

import org.apache.lucene.document.Document;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * Refers from one artifact to another.
 */
public class Reference implements Comparable<Reference> {
    public final Document document;
    public final Pom from;
    public final GAV to;

    public Reference(Document document, Pom from, GAV to) {
        this.document = document;
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Reference)) {
            return false;
        }

        Reference other = (Reference) obj;
        return from.coordinates.toGavString().equals(other.from.coordinates.toGavString());
    }

    @Override
    public int hashCode() {
        return from.coordinates.toGavString().hashCode();
    }

    public int compareTo(Reference other) {
        int result;
        ArtifactVersion left;
        ArtifactVersion right;

        result = from.coordinates.groupId.compareTo(other.from.coordinates.groupId);
        if (result != 0) {
            return result;
        }
        result = from.coordinates.artifactId.compareTo(other.from.coordinates.artifactId);
        if (result != 0) {
            return result;
        }

        left = new DefaultArtifactVersion(from.coordinates.version);
        right = new DefaultArtifactVersion(other.from.coordinates.version);
        return left.compareTo(right);
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
