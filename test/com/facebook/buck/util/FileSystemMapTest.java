/*
 * Copyright 2017-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class FileSystemMapTest {

  private FileSystemMap.ValueLoader<Boolean> loader = path -> true;

  @Test
  public void testPutLeafNodeWithEmptyTrie() {
    Path path = Paths.get("foo/bar/HelloWorld.java");
    FileSystemMap<Boolean> fsMap = new FileSystemMap<>(loader);
    fsMap.put(path, true);
    FileSystemMap.Entry<Boolean> foo = fsMap.root.subLevels.get(Paths.get("foo"));
    assertNotNull(foo);
    FileSystemMap.Entry<Boolean> bar = foo.subLevels.get(Paths.get("bar"));
    assertNotNull(bar);
    FileSystemMap.Entry<Boolean> file = bar.subLevels.get(Paths.get("HelloWorld.java"));
    assertNotNull(file);
    assertTrue(file.value);
    assertEquals(fsMap.map.size(), 1);
    assertTrue(fsMap.map.get(path));
  }

  @Test
  public void testPutLeafNodeWithNonEmptyTrie() {
    Path path = Paths.get("foo/bar/HelloWorld.java");
    FileSystemMap<Boolean> fsMap = new FileSystemMap<>(loader);
    FileSystemMap.Entry<Boolean> usr = new FileSystemMap.Entry<>();
    fsMap.root.subLevels.put(Paths.get("usr"), usr);
    fsMap.map.put(Paths.get("usr"), true);
    fsMap.put(path, true);
    assertEquals(fsMap.root.subLevels.get(Paths.get("usr")).size(), 0);
    FileSystemMap.Entry<Boolean> foo = fsMap.root.subLevels.get(Paths.get("foo"));
    assertNotNull(foo);
    FileSystemMap.Entry<Boolean> bar = foo.subLevels.get(Paths.get("bar"));
    assertNotNull(bar);
    FileSystemMap.Entry<Boolean> file = bar.subLevels.get(Paths.get("HelloWorld.java"));
    assertNotNull(file);
    assertTrue(file.value);
    assertEquals(fsMap.map.size(), 2);
    assertTrue(fsMap.map.get(path));
  }

  @Test
  public void testPutLeafNodeAlreadyInserted() {
    Path path = Paths.get("usr/HelloWorld.java");
    FileSystemMap<Boolean> fsMap = new FileSystemMap<>(loader);
    FileSystemMap.Entry<Boolean> usr = new FileSystemMap.Entry<>();
    fsMap.root.subLevels.put(Paths.get("usr"), usr);
    FileSystemMap.Entry<Boolean> helloWorld = new FileSystemMap.Entry<>(true);
    usr.subLevels.put(Paths.get("HelloWorld.java"), helloWorld);
    fsMap.map.put(path, true);
    fsMap.put(path, false);
    // We check that the object hasn't been reinstantiated => reference is the same.
    assertSame(fsMap.root.subLevels.get(Paths.get("usr")), usr);
    assertSame(usr.subLevels.get(Paths.get("HelloWorld.java")), helloWorld);
    FileSystemMap.Entry<Boolean> helloWorldEntry = usr.subLevels.get(Paths.get("HelloWorld.java"));
    assertNotNull(helloWorldEntry);
    assertFalse(helloWorldEntry.value);
    assertEquals(fsMap.map.size(), 1);
    assertFalse(fsMap.map.get(path));
  }

  @Test
  public void testPutLeafNodePathPartiallyInserted() {
    Path path = Paths.get("usr/HelloWorld.java");
    FileSystemMap<Boolean> fsMap = new FileSystemMap<>(loader);
    FileSystemMap.Entry<Boolean> usr = new FileSystemMap.Entry<>();
    fsMap.root.subLevels.put(Paths.get("usr"), usr);
    fsMap.put(path, true);
    // We check that the object hasn't been reinstantiated => reference is the same.
    assertSame(fsMap.root.subLevels.get(Paths.get("usr")), usr);
    FileSystemMap.Entry<Boolean> file = usr.subLevels.get(Paths.get("HelloWorld.java"));
    assertNotNull(file);
    assertTrue(file.value);
    assertEquals(fsMap.map.size(), 1);
    assertTrue(fsMap.map.get(path));
  }

  @Test
  public void testRemovePathThatExistsAndIntermediateNodesAreRemovedToo() {
    Path path = Paths.get("usr/HelloWorld.java");
    FileSystemMap<Boolean> fsMap = new FileSystemMap<>(loader);
    FileSystemMap.Entry<Boolean> usr = new FileSystemMap.Entry<>();
    fsMap.root.subLevels.put(Paths.get("usr"), usr);
    FileSystemMap.Entry<Boolean> helloWorld = new FileSystemMap.Entry<>(true);
    helloWorld.key = Paths.get("usr/HelloWorld.java");
    usr.subLevels.put(Paths.get("HelloWorld.java"), helloWorld);
    fsMap.map.put(helloWorld.key, true);
    fsMap.remove(path);
    assertFalse(fsMap.root.subLevels.containsKey(Paths.get("usr")));
    assertFalse(usr.subLevels.containsKey(Paths.get("HelloWorld.java")));
    assertEquals(fsMap.map.size(), 0);
  }

  @Test
  public void testRemovePathThatExistsAndIntermediateIsNotRemovedButValueIsRemoved() {
    Path path = Paths.get("usr/HelloWorld.java");
    FileSystemMap<Boolean> fsMap = new FileSystemMap<>(loader);
    FileSystemMap.Entry<Boolean> usr = new FileSystemMap.Entry<>(true);
    usr.key = Paths.get("usr");
    usr.value = true;
    fsMap.root.subLevels.put(Paths.get("usr"), usr);
    fsMap.map.put(usr.key, true);

    FileSystemMap.Entry<Boolean> helloWorld = new FileSystemMap.Entry<>(true);
    helloWorld.key = Paths.get("usr/HelloWorld.java");
    helloWorld.value = true;
    usr.subLevels.put(Paths.get("HelloWorld.java"), helloWorld);
    fsMap.map.put(helloWorld.key, true);

    FileSystemMap.Entry<Boolean> yo = new FileSystemMap.Entry<>(true);
    yo.key = Paths.get("usr/Yo.java");
    yo.value = true;
    usr.subLevels.put(Paths.get("Yo.java"), yo);
    fsMap.map.put(yo.key, true);

    fsMap.remove(path);
    assertTrue(fsMap.root.subLevels.containsKey(Paths.get("usr")));
    assertNull(fsMap.root.subLevels.get(Paths.get("usr")).value);
    assertFalse(usr.subLevels.containsKey(Paths.get("HelloWorld.java")));
    assertTrue(usr.subLevels.containsKey(Paths.get("Yo.java")));
    assertEquals(fsMap.map.size(), 1);
    assertTrue(fsMap.map.get(yo.key));
  }

  @Test
  public void testRemovePathThatDoesntExist() {
    Path path = Paths.get("usr/HelloWorld.java");
    FileSystemMap<Boolean> fsMap = new FileSystemMap<>(loader);
    FileSystemMap.Entry<Boolean> usr = new FileSystemMap.Entry<>();
    fsMap.root.subLevels.put(Paths.get("usr"), usr);
    fsMap.map.put(path, true);
    fsMap.remove(path);
    assertFalse(fsMap.root.subLevels.containsKey(Paths.get("usr")));
    assertEquals(fsMap.map.size(), 1);
    assertTrue(fsMap.map.get(path));
  }

  @Test
  public void testRemoveIntermediateNode() {
    Path path = Paths.get("usr");
    FileSystemMap<Boolean> fsMap = new FileSystemMap<>(loader);
    FileSystemMap.Entry<Boolean> usr = new FileSystemMap.Entry<>(true);
    usr.key = Paths.get("usr");
    usr.value = true;
    fsMap.root.subLevels.put(Paths.get("usr"), usr);
    fsMap.map.put(usr.key, true);

    FileSystemMap.Entry<Boolean> helloWorld = new FileSystemMap.Entry<>(true);
    helloWorld.key = Paths.get("usr/HelloWorld.java");
    helloWorld.value = true;
    usr.subLevels.put(Paths.get("HelloWorld.java"), helloWorld);
    fsMap.map.put(helloWorld.key, true);

    FileSystemMap.Entry<Boolean> yo = new FileSystemMap.Entry<>(true);
    yo.key = Paths.get("usr/Yo.java");
    yo.value = true;
    usr.subLevels.put(Paths.get("Yo.java"), yo);
    fsMap.map.put(yo.key, true);

    fsMap.remove(path);
    assertFalse(fsMap.root.subLevels.containsKey(Paths.get("usr")));
    assertFalse(fsMap.map.containsKey(path));
    assertFalse(fsMap.map.containsKey(Paths.get("usr/HelloWorld.java")));
    assertFalse(fsMap.map.containsKey(Paths.get("usr/Yo.java")));
  }

  @Test
  public void testRemoveAll() {
    FileSystemMap<Boolean> fsMap = new FileSystemMap<>(loader);
    FileSystemMap.Entry<Boolean> usr = new FileSystemMap.Entry<>();
    fsMap.root.subLevels.put(Paths.get("usr"), usr);
    usr.subLevels.put(Paths.get("HelloWorld.java"), new FileSystemMap.Entry<>(true));
    usr.subLevels.put(Paths.get("Yo.java"), new FileSystemMap.Entry<>(true));
    fsMap.map.put(Paths.get("usr"), true);
    fsMap.map.put(Paths.get("usr/HelloWorld.java"), true);
    fsMap.map.put(Paths.get("usr/Yo.java"), true);
    fsMap.removeAll();
    assertEquals(fsMap.root.size(), 0);
    assertEquals(fsMap.map.size(), 0);
  }

  @Test
  public void testRemoveAllWithEmptyTrie() {
    FileSystemMap<Boolean> fsMap = new FileSystemMap<>(loader);
    fsMap.removeAll();
    assertEquals(fsMap.root.size(), 0);
    assertEquals(fsMap.map.size(), 0);
  }

  @Test
  public void testGetWithPathThatExists() throws IOException {
    Path path = Paths.get("usr/HelloWorld.java");
    FileSystemMap<Boolean> fsMap = new FileSystemMap<>(loader);
    FileSystemMap.Entry<Boolean> usr = new FileSystemMap.Entry<>();
    fsMap.root.subLevels.put(Paths.get("usr"), usr);
    FileSystemMap.Entry<Boolean> helloWorld = new FileSystemMap.Entry<>(true);
    usr.subLevels.put(Paths.get("HelloWorld.java"), helloWorld);
    assertTrue(fsMap.get(path));
    assertEquals(fsMap.root.size(), 1);
    assertTrue(fsMap.map.get(path));
  }

  @Test
  public void testGetAtRootLevelWithPathThatExists() throws IOException {
    Path path = Paths.get("HelloWorld.java");
    FileSystemMap<Boolean> fsMap = new FileSystemMap<>(loader);
    FileSystemMap.Entry<Boolean> helloWorld = new FileSystemMap.Entry<>(true);
    fsMap.root.subLevels.put(Paths.get("HelloWorld.java"), helloWorld);
    assertTrue(fsMap.get(path));
    assertEquals(fsMap.root.size(), 1);
    assertTrue(fsMap.map.get(path));
  }

  @Test
  public void testGetWithPathDoesntExist() throws IOException {
    Path path = Paths.get("usr/GoodbyeCruelWorld.java");
    FileSystemMap<Boolean> fsMap = new FileSystemMap<>(loader);
    FileSystemMap.Entry<Boolean> usr = new FileSystemMap.Entry<>();
    fsMap.root.subLevels.put(Paths.get("usr"), usr);
    FileSystemMap.Entry<Boolean> helloWorld = new FileSystemMap.Entry<>(true);
    usr.subLevels.put(Paths.get("HelloWorld.java"), helloWorld);
    fsMap.map.put(Paths.get("usr/HelloWorld.java"), true);
    Boolean entry = fsMap.get(path);
    assertNotNull(entry);
    assertTrue(entry);
    assertEquals(fsMap.map.size(), 2);
    assertTrue(fsMap.map.get(path));
  }
}
