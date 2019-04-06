package com.therandomlabs.vanilladeathchest.config;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.impl.SyntaxError;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.therandomlabs.vanilladeathchest.VanillaDeathChest;
import com.therandomlabs.vanilladeathchest.util.DeathChestPlacer;
import net.fabricmc.fabric.api.event.server.ServerTickCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;

public final class VDCConfig implements ServerTickCallback {
	public static final class Defense {
		@Config.Property("Whether the unlocker should be damaged rather than consumed.")
		public boolean damageUnlockerInsteadOfConsume;

		@Config.Property("Whether defense entities drop experience.")
		public boolean defenseEntityDropsExperience;

		@Config.Property("Whether defense entities drop items.")
		public boolean defenseEntityDropsItems;

		@Config.RangeDouble(min = 0.0)
		@Config.Property(
				"The maximum distance squared that a defense entity can be from its chest."
		)
		public double defenseEntityMaxDistanceSquared = 64.0;

		@Config.RangeDouble(min = 0.0)
		@Config.Property(
				"The maximum distance squared that a defense entity can be from its player if it " +
						"is too far away from its chest."
		)
		public double defenseEntityMaxDistanceSquaredFromPlayer = 64.0;

		@Config.Property("The defense entity NBT data.")
		public String defenseEntityNBT = "{}";

		@Config.Property({
				"The registry name of the defense entity.",
				"If the defense entity is a living entity, it will not automatically despawn.",
				"If the defense entity can have a revenge target, then the revenge target will " +
						"be set to the player that died."
		})
		public String defenseEntityRegistryName = "";

		@Config.RangeInt(min = 1)
		@Config.Property("The number of defense entities to spawn.")
		public int defenseEntitySpawnCount = 3;

		@Config.RangeInt(min = 0, max = Short.MAX_VALUE)
		@Config.Property({
				"How many times the unlocker should be consumed or damaged.",
				"If the unlocker cannot be consumed or damage this many times, the death chest " +
						"will not be unlocked.",
				"Note that only the stack that the player is holding will be consumed, and that " +
						"players in creative mode will not have their unlocker item consumed."
		})
		public int unlockerConsumeAmount = 1;

		@Config.Property("The registry name of the unlocker.")
		public String unlockerRegistryName = "";

		@Config.Property({
				"The message that is sent to the player when they fail to unlock a death chest.",
				"This string takes the required amount and display name of the item as arguments."
		})
		public String unlockFailedMessage =
				"You need %s of the following item to retrieve your items: %s";

		@Config.Property(
				"Whether the unlock failed message should be a status message rather than a " +
						"chat message."
		)
		public boolean unlockFailedStatusMessage = true;

		@Config.Ignore
		public EntityType defenseEntity;

		@Config.Ignore
		public Item unlocker;

		private void reload(JsonObject object) {
			final Identifier entityIdentifier = new Identifier(defenseEntityRegistryName);

			if(Registry.ENTITY_TYPE.getIds().contains(entityIdentifier)) {
				defenseEntity = Registry.ENTITY_TYPE.get(entityIdentifier);
				defenseEntityRegistryName = Registry.ENTITY_TYPE.getId(defenseEntity).toString();
			} else {
				defenseEntity = null;
				defenseEntityRegistryName = "";
			}

			final Identifier itemIdentifier = new Identifier(unlockerRegistryName);
			unlocker = Registry.ITEM.get(itemIdentifier);

			if(unlocker == Items.AIR) {
				unlocker = null;
				unlockerRegistryName = "";
			} else {
				unlockerRegistryName = Registry.ITEM.getId(unlocker).toString();
			}

			final JsonObject category = object.getAsJsonObject("defense");

			category.getAsJsonObject("defenseEntityRegistryName").addProperty(
					"value", defenseEntityRegistryName
			);
			category.getAsJsonObject("unlockerRegistryName").addProperty(
					"value", unlockerRegistryName
			);
		}
	}

	public static final class Misc {
		@Config.Property("Whether death chets should disappear when they are emptied.")
		public boolean deathChestsDisappearWhenEmptied = VanillaDeathChest.IS_DEOBFUSCATED;

		@Config.Property({
				"Whether death chests should be dropped when broken.",
				"Enable this for infinite chests."
		})
		public boolean dropDeathChests;

		@Config.Property("The default value of the spawnDeathChests gamerule.")
		public boolean gameRuleDefaultValue = true;

		@Config.Property({
				"The name of the spawnDeathChests gamerule.",
				"Set this to an empty string to disable the gamerule."
		})
		public String gameRuleName = "spawnDeathChests";

		@Config.RequiresWorldReload
		@Config.Property("Whether to enable the /vdcreload command.")
		public boolean vdcreload = true;
	}

	public static final class Protection {
		@Config.Property(
				"Whether players in creative mode should be able to bypass death chest " +
						"protection."
		)
		public boolean bypassIfCreative = true;

		@Config.RangeInt(min = 0)
		@Config.Property("The required permission level to bypass death chest proteciton.")
		public int bypassPermissionLevel = 3;

		@Config.Property({
				"Whether death chests should be protected.",
				"When this is enabled, death chests can only be broken by their owners."
		})
		public boolean enabled = true;

		@Config.RangeInt(min = 0)
		@Config.Property({
				"The amount of time in ticks death chest protection should last.",
				"120000 ticks is 5 in-game days.",
				"Set this to 0 to protect death chests indefinitely."
		})
		public int period = 120000;
	}

	public static final class Spawning {
		@Config.Property({
				"The message sent to a player when they die and a death chest is placed.",
				"%d refers to the X, Y and Z coordinates.",
				"Set this to an empty string to disable the message."
		})
		public String chatMessage = "Death chest spawned at [%d, %d, %d]";

		@Config.Property({
				"The display name of the death chest container.",
				"Leave this empty to not specify a custom name."
		})
		public String containerDisplayName = "Death Chest";

		@Config.Property("The type of death chest that should be placed.")
		public DeathChestPlacer.DeathChestType chestType =
				DeathChestPlacer.DeathChestType.SINGLE_OR_DOUBLE;

		@Config.Property(
				"Whether to force place a death chest at the location of a player's death if no " +
						"viable locations are found."
		)
		public boolean forcePlaceIfLocationNotFound;

		@Config.RangeInt(min = 1)
		@Config.Property("The death chest location search radius.")
		public int locationSearchRadius = 8;

		@Config.Property("Whether death chests can only spwan on solid blocks.")
		public boolean mustBeOnSolidBlocks;

		@Config.Property("The color of the shulker box if chestType is set to SHULKER_BOX.")
		public DyeColor shulkerBoxColor = DyeColor.WHITE;
	}

	@Config.Ignore
	public static final Path PATH =
			Paths.get("config", VanillaDeathChest.MOD_ID + ".json").toAbsolutePath();

	@Config.Category("Options related to death chest defense.")
	public static final Defense defense = new Defense();

	@Config.Category("Options that don't fit into any other categories.")
	public static final Misc misc = new Misc();

	@Config.Category("Options related to death chest protection.")
	public static final Protection protection = new Protection();

	@Config.Category("Options related to death chest spawning.")
	public static final Spawning spawning = new Spawning();

	private static final Map<String, Category> CATEGORIES = new HashMap<>();

	private static boolean firstReload = true;
	private static boolean reloadedFirstServerTick;

	static {
		try {
			for(Field field : VDCConfig.class.getDeclaredFields()) {
				final int modifiers = field.getModifiers();

				if(!Modifier.isPublic(modifiers) ||
						field.getAnnotation(Config.Ignore.class) != null) {
					continue;
				}

				final Object object = field.get(null);

				CATEGORIES.put(
						field.getName(),
						new Category(
								field, object, object.getClass().getDeclaredFields()
						)
				);
			}
		} catch(Exception ex) {
			VanillaDeathChest.crashReport("Error while getting config properties", ex);
		}
	}

	@Override
	public void tick(MinecraftServer server) {
		if(!reloadedFirstServerTick) {
			//Validate registry names
			reload();
			reloadedFirstServerTick = true;
		}
	}

	public static void reload() {
		JsonObject config = null;

		if(Files.exists(PATH)) {
			try {
				final String raw = Jankson.builder().build().load(
						StringUtils.join(Files.readAllLines(PATH), System.lineSeparator())
				).toJson();

				config = new JsonParser().parse(raw).getAsJsonObject();
			} catch(IOException | SyntaxError | IllegalStateException ex) {
				VanillaDeathChest.crashReport("Failed to read file: " + PATH, ex);
			}
		} else {
			config = new JsonObject();
		}

		try {
			for(Map.Entry<String, Category> category : CATEGORIES.entrySet()) {
				loadCategory(
						getObject(config, category.getKey()),
						category.getValue()
				);
			}
		} catch(IllegalAccessException ex) {
			VanillaDeathChest.crashReport("Failed to reload config", ex);
		}

		//Remove non-category entries

		final List<String> toRemove = new ArrayList<>();

		for(Map.Entry<String, JsonElement> entry : config.entrySet()) {
			final String key = entry.getKey();

			if(!CATEGORIES.containsKey(key) || !entry.getValue().isJsonObject()) {
				toRemove.add(key);
			}
		}

		for(String key : toRemove) {
			config.remove(key);
		}

		onReload(config);

		firstReload = false;

		//Write JSON

		final String raw = new GsonBuilder().
				setPrettyPrinting().
				disableHtmlEscaping().
				create().
				toJson(config).replaceAll(" {2}", "\t");

		try {
			Files.createDirectories(PATH.getParent());
			Files.write(PATH, (raw + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
		} catch(IOException ex) {
			VanillaDeathChest.crashReport("Failed to write to: " + PATH, ex);
		}
	}

	private static void loadCategory(JsonObject object, Category category)
			throws IllegalAccessException {
		addDescription(object, category.getDescription());

		final List<Property> properties = category.getProperties();

		for(Property property : properties) {
			loadProperty(
					category.getObject(),
					getObject(object, property.getKey()),
					property
			);
		}

		//Remove non-property entries

		for(Map.Entry<String, JsonElement> entry : object.entrySet()) {
			final String key = entry.getKey();

			if(key.equals("description")) {
				continue;
			}

			boolean found = false;

			for(Property property : properties) {
				if(property.getKey().equals(key)) {
					found = true;
					break;
				}
			}

			if(!found) {
				object.remove(key);
			}
		}
	}

	@SuppressWarnings("Duplicates")
	private static void loadProperty(Object categoryObject, JsonObject object, Property property)
			throws IllegalAccessException {
		addDescription(object, property.getDescription());

		final Field field = property.getField();
		final Class<?> type = field.getType();

		final Object defaultValue = property.getDefaultValue();
		final JsonElement value = object.get("value");
		Object newValue = null;

		if(property.isEnum()) {
			if(value != null && value.isJsonPrimitive()) {
				final JsonPrimitive primitive = value.getAsJsonPrimitive();

				if(primitive.isString()) {
					final String string = primitive.getAsString();

					for(Object constant : type.getEnumConstants()) {
						if(constant.toString().equals(string)) {
							newValue = constant;
							break;
						}
					}
				}
			}

			if(newValue == null) {
				object.addProperty("value", defaultValue.toString());
			}
		} else if(type == String.class) {
			if(value != null && value.isJsonPrimitive()) {
				final JsonPrimitive primitive = value.getAsJsonPrimitive();

				if(primitive.isString()) {
					newValue = primitive.getAsString();
				}
			}

			if(newValue == null) {
				object.addProperty("value", defaultValue.toString());
			}
		} else if(type == boolean.class) {
			if(value != null && value.isJsonPrimitive()) {
				final JsonPrimitive primitive = value.getAsJsonPrimitive();

				if(primitive.isBoolean()) {
					newValue = primitive.getAsBoolean();
				}
			}

			if(newValue == null) {
				object.addProperty("value", (Boolean) defaultValue);
			}
		} else if(type == int.class) {
			if(value != null && value.isJsonPrimitive()) {
				final JsonPrimitive primitive = value.getAsJsonPrimitive();

				if(primitive.isNumber()) {
					int number = primitive.getAsInt();

					final int min = property.getMinInt();
					final int max = property.getMaxInt();

					if(number < min) {
						number = min;
						object.addProperty("value", number);
					}

					if(number > max) {
						number = max;
						object.addProperty("value", number);
					}

					newValue = number;
				}
			}

			if(newValue == null) {
				object.addProperty("value", (Integer) defaultValue);
			}
		} else if(type == double.class) {
			if(value != null && value.isJsonPrimitive()) {
				final JsonPrimitive primitive = value.getAsJsonPrimitive();

				if(primitive.isNumber()) {
					double number = primitive.getAsDouble();

					final double min = property.getMinDouble();
					final double max = property.getMaxDouble();

					if(number < min) {
						number = min;
						object.addProperty("value", number);
					}

					if(number > max) {
						number = max;
						object.addProperty("value", number);
					}

					newValue = number;
				}
			}

			if(newValue == null) {
				object.addProperty("value", (Double) defaultValue);
			}
		}

		field.set(categoryObject, newValue == null ? defaultValue : newValue);
	}

	private static void addDescription(JsonObject object, String[] description) {
		final JsonArray array = new JsonArray();

		for(String line : description) {
			array.add(line);
		}

		object.add("description", array);
	}

	private static JsonObject getObject(JsonObject object, String key) {
		final JsonElement value = object.get(key);

		if(value != null && value.isJsonObject()) {
			return value.getAsJsonObject();
		}

		final JsonObject newObject = new JsonObject();
		object.add(key, newObject);
		return newObject;
	}

	private static void onReload(JsonObject object) {
		if(!firstReload) {
			defense.reload(object);
		}
	}
}
