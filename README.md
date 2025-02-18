## Tag [2.4.1+1.21.1+NeoForge](https://github.com/IceLitty/alloy-forgery/tree/2.4.1%2B1.21.1%2BNeoForge)
### State: Playable
- Need manually extract controller recipe folder to owo `.\moddata` folder like `\.minecraft\moddata\alloy_forgery\alloy_forges\bricks_forge.json`.
  - Source at main method load `wraith.alloyforgery.forges.ForgeRegistry.Loader.INSTANCE` -> `getDataSubdirectory()` return `alloy_forges`, but owo check folder in jar says not find. Use owo fallback method: find files in folder to load.
- Why forgified-fabric-api mixin `net.minecraft.world.entity.npc.VillagerTrades.EmeraldsForVillagerTypeItem#disableVanillaCheck()`? It's not in 1.21.1 jar... Manually remove mixin json `TradeOffersTypeAwareBuyForOneEmeraldFactoryMixin` to skip that, hopes nothing mods use it.

### Notice:
- Require forgified-fabric-api, owo-lib (neoforge ver.)
- Not test with RMI, use EMI is not problem
- May need use kubejs remove hidden joke recipe:
  ```js
  ServerEvents.recipes(event => {
    event.remove({ id: 'alloy_forgery:super_duper_fun_recipe' });
  });
  ```
- Test with `connector-2.0.0-beta.6+1.21.1-full.jar` `emi-1.1.19+1.21.1+neoforge.jar` `forgified-fabric-api-0.107.0+2.0.22+1.21.1.jar` `owo-lib-neoforge-0.12.15-beta.12+1.21.jar` `NeoForge 1.21.1-97`

## Tag [2.4.2+pre+1.21.1+NeoForge](https://github.com/IceLitty/alloy-forgery/releases/tag/2.4.2%2Bpre%2B1.21.1%2BNeoForge)
### State: Playable
- Still may fix forgified-fabric-api mixin problem?
- Not require manually copy `moddata` used by owo, because experimental branch patch is removed this folder, instead by `alloy_forge` folder.
