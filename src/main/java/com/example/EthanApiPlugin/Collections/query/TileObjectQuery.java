package com.example.EthanApiPlugin.Collections.query;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.EthanApiPlugin.PathFinding.GlobalCollisionMap;
import com.example.EthanApiPlugin.Utility.WorldAreaUtility;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;

import javax.swing.text.html.Option;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TileObjectQuery {
    private List<TileObject> tileObjects;
    static Client client = RuneLite.getInjector().getInstance(Client.class);

    public TileObjectQuery(List<TileObject> tileObjects) {
        this.tileObjects = new ArrayList<>(tileObjects);
    }

    public TileObjectQuery withName(String name) {
        tileObjects =
                tileObjects.stream().filter(tileObject -> {
                    ObjectComposition objectComposition = getObjectComposition(tileObject);
                    if (objectComposition == null)
                        return false;
                    return objectComposition.getName().equals(name);
                }).collect(Collectors.toList());
        return this;
    }
//    public TileObjectQuery withActions(String... actions){
//tileObjects =
//                tileObjects.stream().filter(tileObject -> {
//                    ObjectComposition objectComposition = getObjectComposition(tileObject);
//                    if (objectComposition == null)
//                        return false;
//                    String[] objectActions = objectComposition.getActions();
//                    for (String action : actions) {
//                        if (Arrays.stream(objectActions).filter(Objects::nonNull).anyMatch(a -> a.equalsIgnoreCase(action))) {
//                            return true;
//                        }
//                    }
//                    return false;
//                }).collect(Collectors.toList());
//        return this;
//    }

    public TileObjectQuery withId(int id) {
        tileObjects = tileObjects.stream().filter(tileObject -> tileObject.getId() == id).collect(Collectors.toList());
        return this;
    }

    public TileObjectQuery withAction(String action) {
        tileObjects =
                tileObjects.stream().filter(tileObject ->
                {
                    ObjectComposition objectComposition = getObjectComposition(tileObject);
                    if (objectComposition == null)
                        return false;
                    String[] actions = getObjectComposition(tileObject).getActions();
                    return Arrays.stream(actions).filter(Objects::nonNull).anyMatch(a -> a.equalsIgnoreCase(action));
                }).collect(Collectors.toList());
        return this;
    }

    public TileObjectQuery atLocation(WorldPoint location) {
        tileObjects = tileObjects.stream().filter(tileObject -> tileObject.getWorldLocation().equals(location)).collect(Collectors.toList());
        return this;
    }

    public TileObjectQuery withinDistance(int distance) {
        if (client.getLocalPlayer() == null) {
            return this;
        }
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        tileObjects = tileObjects.stream()
                .filter(tileObject -> tileObject.getWorldLocation().distanceTo(playerLocation) <= distance)
                .collect(Collectors.toList());
        return this;
    }

    public TileObjectQuery withinBounds(WorldPoint min, WorldPoint max){
        int x1 = min.getX();
        int x2 = max.getX();
        int y1 = min.getY();
        int y2 = max.getY();

        tileObjects = tileObjects.stream().filter(tileObject -> {
            int x3 = tileObject.getWorldLocation().getX();
            int y3 = tileObject.getWorldLocation().getY();

            if (x3 > Math.max(x1, x2) || x3 < Math.min(x1, x2)) {
                return false;
            }

            return y3 <= Math.max(y1, y2) && y3 >= Math.min(y1, y2);
        }).collect(Collectors.toList());
        return this;
    }

    public TileObjectQuery atLocation(int x, int y, int plane) {
        WorldPoint p = new WorldPoint(x, y, plane);
        tileObjects =
                tileObjects.stream().filter(tileObject -> tileObject.getWorldLocation().equals(p)).collect(Collectors.toList());
        return this;
    }

    public TileObjectQuery filter(Predicate<? super TileObject> predicate) {
        tileObjects = tileObjects.stream().filter(predicate).collect(Collectors.toList());
        return this;
    }

    public TileObjectQuery nameContains(String name) {
        tileObjects =
                tileObjects.stream().filter(tileObject -> {
                    ObjectComposition comp = getObjectComposition(tileObject);
                    if (comp == null)
                        return false;
                    return comp.getName().contains(name);
                }).collect(Collectors.toList());
        return this;
    }

    public TileObjectQuery idInList(List<Integer> ids) {
        tileObjects = tileObjects.stream().filter(tileObject -> ids.contains(tileObject.getId())).collect(Collectors.toList());
        return this;
    }

    public boolean empty() {
        return tileObjects.size() == 0;
    }

    public List<TileObject> result() {
        return tileObjects;
    }

    public Optional<TileObject> first() {
        return tileObjects.stream().findFirst();
    }

    public Optional<TileObject> nearestToPlayer() {
        if (client.getLocalPlayer() == null) {
            return Optional.empty();
        }
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        return tileObjects.stream()
                .min(Comparator.comparingInt(o -> playerLocation.distanceTo(o.getWorldLocation())));
    }

    public Optional<TileObject> nearestByPath() {
        if (client.getLocalPlayer() == null) {
            return Optional.empty();
        }
        HashMap<WorldPoint, TileObject> map = new HashMap<>();
        WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
        for (TileObject tileObject : tileObjects) {
            List<WorldPoint> adjacentTiles = WorldAreaUtility.objectInteractableTiles(tileObject);
            for (WorldPoint worldPoint : adjacentTiles) {
                if (playerLoc.equals(worldPoint)) {
                    return Optional.of(tileObject);
                }
                map.put(worldPoint, tileObject);
            }
        }

        List<WorldPoint> path = EthanApiPlugin.pathToGoalSetFromPlayerNoCustomTiles(new HashSet<>(map.keySet()));
        if (path == null || path.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(map.get(path.get(path.size() - 1)));
    }

    public Optional<TileObject> nearestToPoint(WorldPoint point) {
        return tileObjects.stream().min(Comparator.comparingInt(o -> point.distanceTo(o.getWorldLocation())));
    }

    public static ObjectComposition getObjectComposition(TileObject tileObject) {
        ObjectComposition comp = client.getObjectDefinition(tileObject.getId());
        if (comp != null) {
            comp = comp.getImpostor();
            if (comp != null) {
                return comp;
            }
        }
        return client.getObjectDefinition(tileObject.getId());
    }

    public static ObjectComposition getObjectComposition(int id) {
        ObjectComposition comp = client.getObjectDefinition(id);
        if (comp != null) {
            comp = comp.getImpostor();
            if (comp != null) {
                return comp;
            }
        }
        return client.getObjectDefinition(id);
    }
}
