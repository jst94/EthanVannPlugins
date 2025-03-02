package com.example.EthanApiPlugin.Collections.query;

import com.example.EthanApiPlugin.Collections.ETileItem;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.EthanApiPlugin.PathFinding.GlobalCollisionMap;
import lombok.SneakyThrows;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TileItemQuery {
    public List<ETileItem> tileItems;
    static Client client = RuneLite.getInjector().getInstance(Client.class);
    static ItemManager itemManager = RuneLite.getInjector().getInstance(ItemManager.class);

    public TileItemQuery(List<ETileItem> tileItems) {
        this.tileItems = new ArrayList<ETileItem>(tileItems);
    }

    public TileItemQuery withId(int id) {
        tileItems = this.tileItems.stream().filter(tileItem -> tileItem.tileItem.getId() == id).collect(Collectors.toList());
        return this;
    }

    @SneakyThrows
    public TileItemQuery withName(String name) {
        tileItems = tileItems.stream().filter(tileItem ->
        {
            try {
                return EthanApiPlugin.itemDefs.get(tileItem.tileItem.getId()).getName().equals(name);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        return this;
    }

    public TileItemQuery nameContains(String name) {
        tileItems = tileItems.stream().filter(tileItem ->
        {
            try {
                return EthanApiPlugin.itemDefs.get(tileItem.tileItem.getId()).getName().contains(name);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        return this;
    }

    public TileItemQuery filter(Predicate<? super ETileItem> predicate) {
        tileItems = tileItems.stream().filter(predicate).collect(Collectors.toList());
        return this;
    }

    public TileItemQuery onlyNoted() {
        tileItems = tileItems.stream().filter(this::isNoted).collect(Collectors.toList());
        return this;
    }

    public TileItemQuery onlyUnnoted() {
        tileItems = tileItems.stream().filter(item -> !isNoted(item)).collect(Collectors.toList());
        return this;
    }

    public TileItemQuery matchesWildCardNoCase(String input) {
        tileItems =
                tileItems.stream().
                        filter(item ->
                        {
                            try {
                                return WildcardMatcher.matches(input.toLowerCase(),
                                        Text.removeTags(EthanApiPlugin.itemDefs.get(item.tileItem.getId()).getName().toLowerCase()));
                            } catch (ExecutionException e) {
                                throw new RuntimeException(e);
                            }
                        }).
                        collect(Collectors.toList());
        return this;
    }

    public TileItemQuery eachItemAboveXValue(int value) {
        tileItems = tileItems.stream().filter(item -> itemManager.getItemPrice(item.getTileItem().getId()) > value).collect(Collectors.toList());
        return this;
    }

    public TileItemQuery stackAboveXValue(int value) {
        tileItems = tileItems.stream().filter(item -> item.getTileItem().getQuantity() * itemManager.getItemPrice(item.getTileItem().getId()) > value).collect(Collectors.toList());
        return this;
    }

    //the 3 methods below are ugly af and might not work
    public TileItemQuery doesNotMatchWildCardNoCase(String input) {
        tileItems =
                tileItems.stream().
                        filter(item ->
                        {
                            try {
                                return !WildcardMatcher.matches(input.toLowerCase(),
                                        Text.removeTags(EthanApiPlugin.itemDefs.get(item.tileItem.getId()).getName().toLowerCase()));
                            } catch (ExecutionException e) {
                                throw new RuntimeException(e);
                            }
                        }).
                        collect(Collectors.toList());
        return this;
    }

    public TileItemQuery itemsMatchingWildcardsNoCase(String... input) {
        List<ETileItem> tileItemsTemp = new ArrayList<ETileItem>();
        for (String s : input) {
            tileItemsTemp.addAll(tileItems.stream().
                    filter(item ->
                    {
                        try {
                            return WildcardMatcher.matches(s.toLowerCase(),
                                    Text.removeTags(EthanApiPlugin.itemDefs.get(item.tileItem.getId()).getName().toLowerCase()));
                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    }).
                    collect(Collectors.toList()));
        }
        tileItems = tileItemsTemp;
        return this;
    }

    public TileItemQuery itemsExcludingMatchingWildcardsNoCase(String... input) {
        List<ETileItem> tileItemsTemp = new ArrayList<ETileItem>();
        for (String s : input) {
            tileItemsTemp.addAll(tileItems.stream().
                    filter(item ->
                    {
                        try {
                            return WildcardMatcher.matches(s.toLowerCase(),
                                    Text.removeTags(EthanApiPlugin.itemDefs.get(item.tileItem.getId()).getName().toLowerCase()));
                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    }).
                    collect(Collectors.toList()));
        }
        tileItems.removeAll(tileItemsTemp);
        return this;
    }


    public boolean empty() {
        return tileItems == null || tileItems.isEmpty();
    }

    public List<ETileItem> result() {
        return tileItems != null ? tileItems : new ArrayList<>();
    }

    public Optional<ETileItem> first() {
        if (tileItems == null || tileItems.isEmpty()) {
            return Optional.empty();
        }
        ETileItem firstItem = tileItems.get(0);
        return Optional.ofNullable(firstItem);
    }

    public TileItemQuery withinDistance(int distance) {
        if (client.getLocalPlayer() == null) {
            return this;
        }
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        tileItems = tileItems.stream()
                .filter(tileItem -> tileItem.getLocation().distanceTo(playerLocation) <= distance)
                .collect(Collectors.toList());
        return this;
    }

    public TileItemQuery withinDistanceToPoint(int distance, WorldPoint point) {
        if (point == null) {
            return this;
        }
        tileItems = tileItems.stream()
                .filter(tileItem -> tileItem.getLocation().distanceTo(point) <= distance)
                .collect(Collectors.toList());
        return this;
    }
    public TileItemQuery alchValueAbove(int value) {
        tileItems = tileItems.stream().filter(tileItem -> {
            ItemComposition comp = itemManager.getItemComposition(tileItem.getTileItem().getId());
            return comp != null && comp.getHaPrice() > value;
        }).collect(Collectors.toList());
        return this;
    }

    public Optional<ETileItem> nearestToPlayer() {
        if (client.getLocalPlayer() == null) {
            return Optional.empty();
        }
        return nearestToPoint(client.getLocalPlayer().getWorldLocation());
    }

    public Optional<ETileItem> nearestToPoint(WorldPoint point) {
        if (tileItems.isEmpty() || point == null) {
            return Optional.empty();
        }
        return tileItems.stream().min(Comparator.comparingInt(tileItem -> tileItem.location.distanceTo(point)));
    }

    @SneakyThrows
    public boolean isNoted(ETileItem item) {
        if (item == null || item.tileItem == null) {
            return false;
        }
        ItemComposition itemComposition = EthanApiPlugin.itemDefs.get(item.tileItem.getId());
        return itemComposition != null && itemComposition.getNote() != -1;
    }

    public Optional<ETileItem> nearestByPath() {
        if (client.getLocalPlayer() == null || tileItems.isEmpty()) {
            return Optional.empty();
        }
        HashSet<WorldPoint> tiles = new HashSet<>();
        HashMap<WorldPoint, ETileItem> map = new HashMap<>();
        WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
        for (ETileItem t : tileItems) {
            if (playerLoc.equals(t.getLocation())) {
                return Optional.of(t);
            }
            map.put(t.getLocation(), t);
            tiles.add(t.getLocation());
        }
        List<WorldPoint> path = EthanApiPlugin.pathToGoalSetFromPlayerNoCustomTiles(new HashSet<>(tiles));
        if (path == null || path.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(map.get(path.get(path.size() - 1)));
    }
}
