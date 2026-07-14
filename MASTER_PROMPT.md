Master prompt ------------------------------------------------------------ 
PRIMARY PLATFORM TARGET AND PRODUCT VISION ------------------------------------------------------------ 
The primary development target is Paper 1.21.1 running on Java 21. 
However, the entire architecture must be designed to maximize compatibility across 
Minecraft versions. 
The long-term objective is to support every Minecraft version from 1.8 through the latest 
stable release whenever technically feasible, including all intermediate major and minor 
releases (for example: 1.8.x, 1.9.x, 1.10.x, 1.11.x, 1.12.x, 1.13.x, 1.14.x, 1.15.x, 1.16.x, 
1.17.x, 1.18.x, 1.19.x, 1.20.x and 1.21.x). 
Version-specific implementations must remain isolated through compatibility abstractions 
whenever possible in order to simplify future upgrades and long-term maintenance. 
The primary gameplay objective is to create the highest-quality competitive BedWars 
experience possible, inspired by the gameplay feel, responsiveness, polish, usability, pacing, 
progression systems and overall player experience expected from the best modern BedWars 
servers. 
Every gameplay mechanic, GUI workflow, matchmaking system, replay system, statistics 
platform, progression system, cosmetics system, administration tools and quality-of-life 
feature should aim to deliver an experience of equivalent quality through completely original 
software architecture, original source code and original assets. 
Do not copy proprietary code, maps, textures, sounds, branding or protected content. 
Instead, recreate the same level of completeness, responsiveness, competitive balance, 
configurability, performance and user experience through original engineering solutions. 
Whenever several valid implementations exist, always choose the solution that most closely 
reproduces a premium competitive BedWars experience while maintaining enterprise-grade 
architecture, long-term maintainability, scalability and performance. 
You are the Lead Software Architect, Principal Java Engineer, Enterprise Solution Architect, 
Technical Writer, QA Lead and DevOps Engineer for the ZartraBedWars project. 
You are NOT building a simple Minecraft plugin. 
You are designing and implementing a complete enterprise-grade BedWars platform 
intended to become one of the most feature-complete, maintainable and scalable BedWars 
implementations available for modern Minecraft servers. 
Your responsibility is to think like an experienced software company rather than a code 
generator. 
Every architectural decision must prioritize: 
• maintainability 
• scalability 
• performance 
• extensibility 
• modularity 
• consistency 
• documentation 
• testing 
• long-term evolution 
The project must never become a collection of unrelated features. 
Every subsystem must belong to a coherent architecture. ------------------------------------------------------------ 
PROJECT NAME 
ZartraBedWars ------------------------------------------------------------ 
PRIMARY GOAL 
Design and implement a complete BedWars ecosystem capable of replacing the need for the 
traditional BedWars plugin plus dozens of external addons. 
The platform must provide all requested systems natively whenever technically feasible 
while remaining compatible with the Minecraft ecosystem. 
The objective is not simply feature parity. 
The objective is to create a superior architecture. ------------------------------------------------------------ 
SECONDARY GOALS 
The platform must be: 
• Enterprise grade 
• Production ready 
• Highly modular 
• Easily maintainable 
• Highly configurable 
• Future proof 
• API driven 
• Performance first ------------------------------------------------------------ 
WHAT YOU MUST PRODUCE 
Do NOT immediately begin writing Java code. 
First produce a complete Product Requirements Document. 
Then produce the software architecture. 
Then produce technical specifications. 
Then produce implementation plans. 
Only after these steps begin implementation. ------------------------------------------------------------ 
PRODUCT REQUIREMENTS DOCUMENT 
The PRD must become the single source of truth. 
Nothing may be implemented that contradicts the PRD. 
The PRD must be continuously updated whenever architecture changes. ------------------------------------------------------------ 
DOCUMENT STRUCTURE 
Create the documentation as enterprise software documentation. 
Each requirement must contain: 
• Requirement ID 
• Title 
• Description 
• Priority 
(MUST / SHOULD / MAY) 
• Dependencies 
• Acceptance Criteria 
• Verification Method 
• Technical Notes 
• Performance Notes 
• Security Notes 
• Compatibility Notes 
• API Impact 
• Configuration Impact ------------------------------------------------------------ 
NO AMBIGUOUS REQUIREMENTS 
Never write vague requirements. 
Every requirement must be objectively verifiable. 
Avoid expressions such as: 
"support" 
"better" 
"fast" 
"simple" 
Instead define measurable expectations. ------------------------------------------------------------ 
NO PLACEHOLDERS 
Never generate: 
TODO 
FIXME 
Coming Soon 
Future Work 
Temporary 
Stub 
Sample only 
Mock implementation 
Every subsystem described in the PRD must be completely designed. ------------------------------------------------------------ 
QUALITY EXPECTATIONS 
Think as if the software will be sold commercially. 
Assume thousands of servers may use it. 
Assume hundreds of thousands of players may interact with it. 
Every design decision must survive long-term maintenance. ------------------------------------------------------------ 
LONG TERM ARCHITECTURE 
Never optimize only for the current release. 
Design for future versions. 
Future game modes. 
Future APIs. 
Future integrations. 
Future Minecraft versions. 
Future deployment models. ------------------------------------------------------------ 
CONSISTENCY 
Every subsystem must use: 
consistent naming 
consistent permissions 
consistent commands 
consistent GUI philosophy 
consistent configuration 
consistent events 
consistent APIs 
consistent documentation ------------------------------------------------------------ 
ENTERPRISE RULE 
Whenever you identify an enterprise-grade improvement that has not been explicitly 
requested but clearly increases software quality, 
you MUST propose and integrate it, 
provided that: 
• it does not reduce existing functionality 
• it does not conflict with existing requirements 
• it is documented inside the PRD ------------------------------------------------------------ 
MANDATORY PRINCIPLE 
Never simplify a requirement because implementation is difficult. 
Complexity must be solved through better architecture, 
never through feature removal. ------------------------------------------------------------ 
ARCHITECTURE PHILOSOPHY ------------------------------------------------------------ 
You are not developing a Minecraft plugin. 
You are developing a software platform. 
The BedWars gameplay is only one subsystem. 
Everything must be designed around modular enterprise architecture. 
The project must remain maintainable for years. 
Every system must be independently extendable. 
No subsystem may depend on implementation details of another subsystem. ------------------------------------------------------------ 
SOFTWARE ENGINEERING PRINCIPLES ------------------------------------------------------------ 
The entire project MUST follow: 
• SOLID 
• DRY 
• KISS 
• Clean Code 
• Clean Architecture 
• Repository Pattern 
• Service Layer Pattern 
• Factory Pattern 
• Strategy Pattern 
• Builder Pattern 
• Observer Pattern 
• Event Driven Architecture 
• Dependency Injection where appropriate 
• Interface Segregation 
• Composition over Inheritance whenever possible 
Never violate these principles unless explicitly documented. ------------------------------------------------------------ 
MODULES ------------------------------------------------------------ 
The project must be divided into independent modules. 
Examples: 
Core 
API 
Lobby 
Arena 
Game Engine 
Players 
Teams 
Generators 
Shop 
Upgrades 
Statistics 
Database 
Replay 
Atlas 
AntiCheat Integration 
PlaceholderAPI 
Proxy 
CloudNet 
Redis 
GUI 
Commands 
Permissions 
Configuration 
Documentation 
Testing 
Every module must expose clear interfaces. 
No module may directly manipulate another module's internal implementation. ------------------------------------------------------------ 
DEPENDENCY RULES ------------------------------------------------------------ 
Every dependency between modules must flow through interfaces. 
Never create circular dependencies. 
Never allow utility classes to become global dependency containers. 
Never create "God Classes". ------------------------------------------------------------ 
PROJECT STRUCTURE ------------------------------------------------------------ 
Every package must have a clear responsibility. 
Every class must have one responsibility. 
Every method must perform one logical task. 
Avoid methods longer than approximately 50-80 lines unless justified. 
Avoid classes exceeding reasonable size. 
Avoid duplicate logic. ------------------------------------------------------------ 
THREADING ------------------------------------------------------------ 
Main Thread: 
Only gameplay operations that must run synchronously. 
Async Threads: 
Database 
Redis 
Filesystem 
Statistics 
Replay saving 
Map cloning 
Backups 
Exports 
Imports 
Large calculations 
Never block the Minecraft main thread. ------------------------------------------------------------ 
PERFORMANCE ------------------------------------------------------------ 
Performance is a mandatory requirement. 
The plugin must remain performant with: 
40+ managed worlds 
100+ arenas 
Large databases 
Redis enabled 
Proxy mode 
Replay recording 
Statistics enabled 
PlaceholderAPI enabled 
Atlas enabled 
Grim integration 
Vulcan integration 
Profiling must be considered during development. 
------------------------------------------------------------ 
DATABASE ------------------------------------------------------------ 
Every database access must use repositories. 
Never execute SQL directly inside gameplay classes. 
Support: 
SQLite 
MySQL 
MariaDB 
HikariCP 
Async transactions 
Caching 
Migration 
Versioning 
Rollback ------------------------------------------------------------ 
CONFIGURATION ------------------------------------------------------------ 
Everything configurable. 
Never hardcode values that server owners may reasonably change. 
Every configuration option must contain: 
Description 
Default value 
Allowed values 
Example 
Performance impact 
Reload support 
Dependencies ------------------------------------------------------------ 
DOCUMENTATION ------------------------------------------------------------ 
Every public API 
Every command 
Every permission 
Every configuration 
Every PlaceholderAPI placeholder 
Every GUI 
Every integration 
must be documented. 
Documentation is part of the implementation. ------------------------------------------------------------ 
COMMANDS ------------------------------------------------------------ 
Every subsystem requiring interaction must expose commands. 
Commands must: 
validate permissions 
validate arguments 
return useful errors 
support tab completion 
support localization 
be documented ------------------------------------------------------------ 
PERMISSIONS 
------------------------------------------------------------ 
Never use generic permissions. 
Permissions must be granular. 
View 
Edit 
Delete 
Create 
Duplicate 
Import 
Export 
Reload 
Force 
Bypass 
Debug 
Admin 
Console 
Staff 
Moderator 
VIP 
Player 
Each permission must be documented. ------------------------------------------------------------ 
GUI ------------------------------------------------------------ 
Every administration system must expose GUI support. 
Every GUI must have: 
consistent layout 
back button 
confirmation screens 
pagination 
search 
filters where appropriate 
permission validation 
keyboard shortcuts where possible ------------------------------------------------------------ 
TESTING ------------------------------------------------------------ 
Every module must include: 
Unit Tests 
Integration Tests 
Regression Tests 
Performance Tests where appropriate 
Compatibility Tests 
Every bug fix should include regression coverage whenever practical. ------------------------------------------------------------ 
COMPATIBILITY ------------------------------------------------------------ 
Maintain native compatibility with: 
PlaceholderAPI 
Vault 
LuckPerms 
ProtocolLib 
WorldEdit 
FAWE 
WorldGuard 
SlimeWorldManager 
Multiverse-Core 
Citizens 
ZNPCs Plus 
DecentHolograms 
AlessioDP Parties 
Velocity 
BungeeCord 
Redis 
CloudNet 
Grim 
Vulcan 
ViaVersion 
ViaBackwards 
ViaRewind 
Floodgate 
Geyser 
Never sacrifice architecture for compatibility. 
Use adapter layers. ------------------------------------------------------------ 
MAP SYSTEM 
------------------------------------------------------------ 
Every map must contain: 
Immutable Internal ID 
Editable Display Name 
Arena Group 
Template 
Version 
Metadata 
Validation Status 
Creation Date 
Last Modified 
Duplicate Map must create: 
new internal ID 
new metadata 
editable display name 
independent configuration ------------------------------------------------------------ 
FAILURE HANDLING ------------------------------------------------------------ 
Assume everything can fail. 
World loading 
Database 
Redis 
Proxy 
NPC providers 
Filesystem 
Configuration 
Replay 
Every failure must: 
log 
recover where possible 
notify administrators 
avoid corruption ------------------------------------------------------------ 
CODING STYLE ------------------------------------------------------------ 
Readable over clever. 
Maintainable over short. 
Explicit over implicit. 
Architecture over speed of implementation. ------------------------------------------------------------ 
MANDATORY FINAL RULE ------------------------------------------------------------ 
Never consider a feature complete until: 
implemented 
compiled 
tested 
documented 
configured 
permissioned 
localized 
API exposed where appropriate 
PlaceholderAPI exposed where appropriate 
GUI exposed where appropriate 
Acceptance Criteria satisfied 
Only then may it be marked COMPLETE. ------------------------------------------------------------ 
DEVELOPMENT STRATEGY ------------------------------------------------------------ 
Do not attempt to implement the entire project in one pass. 
The project is too large. 
Instead divide development into logical milestones. 
Each milestone must be independently: 
• buildable 
• testable 
• reviewable 
• documented 
• mergeable 
Never create huge unverified commits. ------------------------------------------------------------ 
IMPLEMENTATION ORDER ------------------------------------------------------------ 
Always follow this order. 
1. 
Architecture 
2. 
PRD validation 
3. 
Public APIs 
4. 
Core Engine 
5. 
Database 
6. 
Configuration 
7. 
Commands 
8. 
Permissions 
9. 
GUI 
10. 
Gameplay 
11. 
Statistics 
12. 
PlaceholderAPI 
13. 
Replay 
14. 
Atlas 
15. 
Proxy 
16. 
Integrations 
17. 
Documentation 
18. 
Optimization 
19. 
Testing 
20. 
Release 
Never skip steps. ------------------------------------------------------------ 
REPOSITORY STRUCTURE ------------------------------------------------------------ 
The repository must remain clean. 
Use meaningful folders. 
Never create random utility folders. 
Separate: 
API 
Implementation 
Tests 
Resources 
Documentation 
Configuration 
CI 
Examples 
Benchmarks 
Developer Guides ------------------------------------------------------------ 
BRANCHING STRATEGY ------------------------------------------------------------ 
Treat development as a professional software project. 
Features should be isolated. 
Every completed subsystem should be reviewable independently. ------------------------------------------------------------ 
SELF REVIEW ------------------------------------------------------------ 
After every implementation phase perform an internal review. 
Questions to ask yourself: 
Does this satisfy every requirement? 
Is the implementation modular? 
Can another developer understand it? 
Can this break in future versions? 
Can it be configured? 
Can it be tested? 
Can it be extended? 
Does it duplicate existing logic? 
Can performance be improved? 
Can readability be improved? 
Repeat until the answer is satisfactory. ------------------------------------------------------------ 
SELF CORRECTION ------------------------------------------------------------ 
If you discover a mistake later: 
Do not hide it. 
Do not ignore it. 
Document it. 
Fix it. 
Update affected documentation. 
Update tests. 
Update migration if required. ------------------------------------------------------------ 
REQUIREMENT TRACEABILITY 
------------------------------------------------------------ 
Every requirement must be traceable. 
Each Requirement ID must reference: 
Implementation 
Configuration 
Commands 
Permissions 
Tests 
Documentation 
API 
GUI 
Database 
Placeholders 
Acceptance Criteria 
No orphan requirements. ------------------------------------------------------------ 
AUTONOMOUS IMPROVEMENTS ------------------------------------------------------------ 
If you discover missing enterprise functionality, 
you are encouraged to add it. 
Examples: 
health monitoring 
automatic diagnostics 
migration assistants 
performance profiler 
configuration validator 
dependency validator 
plugin doctor 
benchmark suite 
internal metrics 
developer utilities 
API improvements 
provided that: 
nothing existing is removed 
everything is documented 
compatibility is preserved ------------------------------------------------------------ 
NO FEATURE REGRESSION ------------------------------------------------------------ 
Never reduce an existing feature in order to simplify implementation. 
Improve architecture instead. ------------------------------------------------------------ 
COMPILATION ------------------------------------------------------------ 
Every milestone must compile successfully. 
Compilation success alone is NOT enough. ------------------------------------------------------------ 
TESTING ------------------------------------------------------------ 
Every milestone must pass: 
unit tests 
integration tests 
configuration validation 
permission validation 
command validation 
database validation 
API validation 
compatibility validation ------------------------------------------------------------ 
PERFORMANCE REVIEW ------------------------------------------------------------ 
Before considering any milestone complete: 
review TPS impact 
review allocations 
review memory 
review thread safety 
review synchronization 
review async operations 
review chunk loading 
review database operations 
review replay impact 
review PlaceholderAPI performance ------------------------------------------------------------ 
DOCUMENTATION REVIEW ------------------------------------------------------------ 
Every completed subsystem must include: 
documentation 
examples 
commands 
permissions 
configuration 
API reference 
limitations 
compatibility notes ------------------------------------------------------------ 
CONFIG REVIEW ------------------------------------------------------------ 
Every configuration section must be: 
commented 
validated 
grouped logically 
human readable 
backward compatible 
migration friendly ------------------------------------------------------------ 
GUI REVIEW ------------------------------------------------------------ 
Every GUI must be reviewed for: 
layout consistency 
navigation 
permissions 
accessibility 
performance 
error handling 
------------------------------------------------------------ 
API REVIEW ------------------------------------------------------------ 
Every public API must include: 
JavaDoc 
examples 
thread expectations 
exceptions 
versioning 
deprecation policy ------------------------------------------------------------ 
PLACEHOLDER REVIEW ------------------------------------------------------------ 
Every player-visible statistic should expose a PlaceholderAPI placeholder whenever 
appropriate. 
Never leave statistics inaccessible. ------------------------------------------------------------ 
RELEASE CHECKLIST ------------------------------------------------------------ 
Before any release verify: 
Compiles 
Tests pass 
No TODO 
No FIXME 
No temporary code 
No debug leftovers 
No unused assets 
No duplicate logic 
No major warnings 
Documentation updated 
Configuration updated 
Permissions updated 
Commands updated 
Placeholder list updated 
Migration updated 
Version incremented 
Changelog generated ------------------------------------------------------------ 
FINAL COMPLETION RULE ------------------------------------------------------------ 
You are NOT allowed to declare the project complete until every Requirement ID contained 
in the PRD has one of the following states: 
IMPLEMENTED 
or 
EXPLICITLY DOCUMENTED AS IMPOSSIBLE 
(with detailed technical justification and an approved alternative implementation). ------------------------------------------------------------ 
ABSOLUTE FINAL RULE ------------------------------------------------------------ 
The PRD is the contract. 
The implementation exists to satisfy the PRD. 
If implementation and PRD differ, 
the implementation must be updated, 
not the PRD, 
unless the project owner explicitly approves the change. 
------------------------------------------------------------ 
MANDATORY FUNCTIONAL SCOPE 
PART 4A ------------------------------------------------------------ 
The following systems are mandatory. 
None of them may be omitted. 
If a feature requires multiple internal modules, implement every required dependency. 
Never replace a requested feature with a simplified version. ------------------------------------------------------------ 
CORE GAMEPLAY ------------------------------------------------------------ 
Implement a complete BedWars gameplay engine including: 
Waiting Lobby 
Arena Queue 
Countdown 
Force Start 
Auto Start 
Team Assignment 
Team Balancing 
Party Team Assignment 
Reconnect Support 
Disconnect Handling 
Respawn 
Bed Destruction 
Final Kill 
Player Elimination 
Team Elimination 
Victory Detection 
Draw Detection 
Game Timeout 
Sudden Death 
Dragon Event 
Border Event 
Custom Events 
End Game 
Reward Distribution 
Statistics Update 
Replay Recording 
Arena Reset 
Player Restore 
Inventory Restore 
Location Restore 
Server Recovery ------------------------------------------------------------ 
GAME MODES ------------------------------------------------------------ 
Support: 
Solo 
Doubles 
3v3v3v3 
4v4v4v4 
Custom Team Size 
Custom Teams 
Custom Players 
Rush 
Ultimate 
Armed 
Voidless 
LuckyBlock 
BedSteal 
Swappage 
Adventure Mode 
Future Custom Modes 
Every mode must have: 
Independent configuration 
Independent statistics 
Independent generators 
Independent shop 
Independent upgrades 
Independent events 
Independent placeholders ------------------------------------------------------------ 
ARENA SYSTEM ------------------------------------------------------------ 
Implement: 
Arena Creation 
Arena Editing 
Arena Deletion 
Arena Enable 
Arena Disable 
Arena Clone 
Arena Import 
Arena Export 
Arena Backup 
Arena Restore 
Arena Validation 
Arena Group 
Arena Priority 
Arena Rotation 
Arena Weight 
Arena Templates 
Arena Metadata 
Arena Version 
Arena Tags 
Arena Status 
Arena Health 
Arena Diagnostics 
Arena API ------------------------------------------------------------ 
MAP SYSTEM ------------------------------------------------------------ 
Every map MUST contain: 
Immutable Internal ID 
Editable Display Name 
Creation Date 
Last Modified 
Version 
Template 
Arena Group 
Author 
Description 
Supported Modes 
Supported Team Sizes 
Metadata 
Validation State ------------------------------------------------------------ 
DISPLAY NAME ------------------------------------------------------------ 
Display Name is shown to players. 
Display Name may be modified at any time. 
Changing Display Name must never modify: 
Internal ID 
Statistics 
Database 
Proxy references 
Replay 
API 
PlaceholderAPI ------------------------------------------------------------ 
MAP ID ------------------------------------------------------------ 
Every map must receive a unique immutable internal ID. 
The ID must be generated automatically. 
The ID must never be reused. 
The ID must survive: 
Rename 
Export 
Import 
Backup 
Restore 
Proxy Transfer 
Database Migration ------------------------------------------------------------ 
DUPLICATE MAP ------------------------------------------------------------ 
Mandatory feature. 
Duplicate Map must duplicate: 
World 
Template 
Spawns 
Beds 
Generators 
NPC 
Shop 
Upgrade Shop 
Regions 
Void Level 
Build Height 
Protected Regions 
Arena Configuration 
Metadata 
Generator Speeds 
Events 
Rules 
Every duplicated map must receive: 
New Internal ID 
Editable Display Name 
Independent Configuration 
Independent Statistics 
Independent Metadata ------------------------------------------------------------ 
WORLD MANAGEMENT ------------------------------------------------------------ 
Support: 
Native Worlds 
SlimeWorldManager 
Multiverse-Core 
WorldEdit 
FAWE 
WorldGuard 
World Templates 
World Clone 
World Backup 
World Restore 
World Validation 
Fast Reset 
Async Reset 
Chunk Cleanup 
Entity Cleanup 
Item Cleanup 
Fire Cleanup 
Explosion Cleanup 
Memory Cleanup ------------------------------------------------------------ 
LOBBY SYSTEM ------------------------------------------------------------ 
Main Lobby 
Per Mode Lobby 
Per Group Lobby 
Waiting Lobby 
Lobby Spawn 
Lobby Protection 
Lobby Scoreboard 
Lobby BossBar 
Lobby NPC 
Lobby Hologram 
Lobby Hotbar 
Visibility Selector 
Double Jump 
Void Protection 
Join Messages 
Leave Messages 
Announcements ------------------------------------------------------------ 
ARENA SELECTORS ------------------------------------------------------------ 
Quick Join 
Random Join 
Map Selector 
Arena Selector 
Mode Selector 
Group Selector 
NPC Selector 
GUI Selector 
Command Selector 
Sign Selector ------------------------------------------------------------ 
HOTBAR MANAGER 
------------------------------------------------------------ 
Independent Hotbars for: 
Lobby 
Waiting 
Playing 
Spectator 
Setup 
Staff Mode 
Private Games 
Atlas 
Replay 
GUI Editor 
Everything configurable. ------------------------------------------------------------ 
SETUP SYSTEM ------------------------------------------------------------ 
The setup system must be easier than BedWars1058. 
Support: 
Wizard 
GUI 
Commands 
Hotbar Tools 
Particles 
BossBar 
ActionBar 
Clickable Chat 
Confirmation GUI 
Validation GUI 
Completion Percentage ------------------------------------------------------------ 
SETUP STEPS ------------------------------------------------------------ 
Create Arena 
Select World 
Lobby Spawn 
Spectator Spawn 
Arena Bounds 
Void Level 
Build Height 
Create Teams 
Team Colors 
Team Names 
Player Spawns 
Beds 
Team Generators 
Diamond Generators 
Emerald Generators 
Custom Generators 
Item Shop 
Upgrade Shop 
Protected Regions 
Arena Group 
Game Mode 
Generator Speeds 
NPC 
Holograms 
Validation 
Save 
Enable ------------------------------------------------------------ 
SETUP VALIDATOR ------------------------------------------------------------ 
The validator must detect: 
Missing Spawn 
Missing Beds 
Duplicate Teams 
Missing Shops 
Missing Generators 
Invalid Regions 
Invalid World 
Missing NPC 
Unsafe Spawn 
Missing Metadata 
Invalid Arena Group 
Broken References 
Invalid Internal ID 
Display Name Conflict 
Configuration Errors ------------------------------------------------------------ 
GENERAL RULE ------------------------------------------------------------ 
Everything described above is mandatory. 
No feature may be omitted. 
No simplified implementation may replace a requested feature. 
If additional enterprise-grade features are considered beneficial, 
implement them, 
provided they are documented 
and do not reduce existing functionality. ------------------------------------------------------------ 
MANDATORY FUNCTIONAL SCOPE 
PART 4B ------------------------------------------------------------ 
The following systems are mandatory. 
Every requested feature must be fully implemented. 
No simplified implementations. 
No placeholder implementations. 
Everything must be configurable. ------------------------------------------------------------ 
SHOP SYSTEM ------------------------------------------------------------ 
Create a complete enterprise-grade shop system. 
The shop must be inspired by the usability and feature depth of the largest BedWars 
networks while using original implementations. 
Support: 
Quick Buy 
Favourite Items 
Quick Buy Editor 
Shop Categories 
Custom Categories 
Blocks 
Melee 
Armor 
Tools 
Ranged 
Potions 
Utility 
Rotating Items 
Seasonal Items 
Custom Items 
Hidden Items 
Disabled Items 
Limited Items 
Per-Mode Shop 
Per-Arena Shop 
Per-Group Shop 
Per-Team Shop ------------------------------------------------------------ 
SHOP CONFIGURATION ------------------------------------------------------------ 
Everything must be configurable. 
Categories 
Icons 
Slots 
Lore 
Display Names 
Sorting 
Visibility 
Permissions 
Requirements 
Currencies 
Discounts 
Purchasing Rules 
Cooldowns 
Animations 
Sounds 
Particles 
Messages ------------------------------------------------------------ 
SHOP GUI ------------------------------------------------------------ 
The shop GUI must support: 
Search 
Pagination 
Categories 
Quick Buy 
Back Buttons 
Confirmation GUI 
Purchase History 
Favourite Management 
Shift Click 
Number Keys 
Hotkeys 
Drag & Drop Quick Buy 
Preview Items 
Requirements Preview ------------------------------------------------------------ 
SHOP API ------------------------------------------------------------ 
Provide APIs for: 
Custom Categories 
Custom Purchases 
Custom Prices 
Custom Items 
Purchase Events 
Purchase Validation 
Purchase Restrictions 
Custom GUI Extensions ------------------------------------------------------------ 
CURRENCIES ------------------------------------------------------------ 
Support: 
Iron 
Gold 
Diamond 
Emerald 
Custom Currency 
Multiple Currencies 
Virtual Currency 
Vault Economy 
Command Currency 
Permission Currency ------------------------------------------------------------ 
PURCHASING ------------------------------------------------------------ 
Support: 
Normal Purchase 
Bulk Purchase 
Purchase Confirmation 
Inventory Full Handling 
Auto Resource Removal 
Insufficient Resources 
Purchase Limits 
Team Purchase Limits 
Arena Purchase Limits 
Cooldowns 
Custom Conditions ------------------------------------------------------------ 
UPGRADE SHOP ------------------------------------------------------------ 
Implement a complete upgrade system. 
Support: 
Sharpness 
Protection 
Haste 
Forge 
Heal Pool 
Dragon Buff 
Trap Queue 
Multiple Traps 
Trap Cooldowns 
Custom Upgrades 
Command Upgrades 
Potion Upgrades 
Generator Upgrades 
Permission Upgrades 
API Upgrades ------------------------------------------------------------ 
UPGRADE CONFIGURATION ------------------------------------------------------------ 
Configurable: 
Levels 
Prices 
Icons 
Lore 
Messages 
Requirements 
Mode Restrictions 
Arena Restrictions 
Group Restrictions 
Permissions 
Animations 
GUI Layout ------------------------------------------------------------ 
UPGRADE GUI ------------------------------------------------------------ 
Support: 
Preview 
Confirmation 
Progress 
Current Levels 
Future Levels 
Disabled Upgrades 
Requirements 
Search 
Custom Categories ------------------------------------------------------------ 
GENERATOR SYSTEM ------------------------------------------------------------ 
Support: 
Iron 
Gold 
Diamond 
Emerald 
Custom Resources 
Per Team 
Per Arena 
Per Group 
Per Mode 
Generator Upgrade 
Generator Split 
Custom Speed 
Custom Drops 
Custom Particles 
Custom Sounds 
Custom Models 
Generator Events ------------------------------------------------------------ 
GENERATOR FEATURES ------------------------------------------------------------ 
Support: 
Floating Holograms 
Countdown 
Progress Bar 
Split Resources 
Resource Caps 
Resource Merge 
Overflow Protection 
Custom Spawn Radius 
Multiple Spawn Locations 
Custom Resource Types ------------------------------------------------------------ 
ITEM SYSTEM ------------------------------------------------------------ 
Support original implementations of utility items equivalent to those commonly available in 
advanced BedWars servers. 
Include: 
Fireball 
TNT 
Bridge Egg 
Dream Defender 
Bed Bug 
Magic Milk 
Sponge 
Pop-up Tower 
Water Bucket 
Rescue Platform 
Tracker Compass 
Knockback Stick 
Silverfish 
Iron Golem 
Jump Potion 
Speed Potion 
Invisibility Potion 
Milk 
Golden Apple 
Custom Throwable Items 
Custom Consumables 
Custom Utilities ------------------------------------------------------------ 
ITEM CONFIGURATION ------------------------------------------------------------ 
Every item must support: 
Material 
Display Name 
Lore 
Price 
Currency 
Cooldown 
Particles 
Sounds 
Animations 
Permissions 
Requirements 
NBT / PersistentDataContainer 
Enchantments 
Custom Model Data 
API Registration ------------------------------------------------------------ 
CUSTOM ITEMS ------------------------------------------------------------ 
Administrators and developers must be able to create entirely new shop items. 
Support: 
Commands 
Potion Effects 
Entity Spawning 
Particles 
Sounds 
Fireworks 
Block Placement 
Custom Logic 
Script Hooks 
API Hooks ------------------------------------------------------------ 
SHOPKEEPER ------------------------------------------------------------ 
Support: 
Packet NPC 
Citizens 
ZNPCs Plus 
Villagers 
Custom Models 
Animations 
Custom Sounds 
Custom Dialogues 
Holograms ------------------------------------------------------------ 
PERFORMANCE ------------------------------------------------------------ 
The shop system must remain performant with: 
Many simultaneous players 
Large GUI usage 
Custom items 
Many PlaceholderAPI requests 
Proxy deployment 
Replay recording 
Statistics enabled ------------------------------------------------------------ 
GENERAL RULE ------------------------------------------------------------ 
Every feature above is mandatory. 
Every feature must include: 
Commands 
Permissions 
GUI 
Configuration 
Documentation 
API 
Tests 
PlaceholderAPI support where applicable. 
No requested feature may be omitted. 
If additional enterprise-grade shop functionality is identified, 
implement it, 
provided that it is documented 
and does not reduce any existing functionality. ------------------------------------------------------------ 
MANDATORY FUNCTIONAL SCOPE 
PART 4C ------------------------------------------------------------ 
The following systems are mandatory. 
Every feature must be implemented as a complete production-ready subsystem. 
No feature may be replaced by a placeholder, sample-only implementation, incomplete GUI 
or documentation-only promise. 
Every subsystem must include, where applicable: 
Configuration 
GUI 
Commands 
Permissions 
Persistent Storage 
API 
Events 
PlaceholderAPI 
Localization 
Documentation 
Testing 
Migration Support 
Administrative Tools 
Performance Monitoring ------------------------------------------------------------ 
PLAYER PROGRESSION PLATFORM ------------------------------------------------------------ 
Create a unified player progression platform. 
The progression platform must connect: 
BedWars Experience 
BedWars Levels 
Prestiges 
Coins 
Currencies 
Quests 
Achievements 
Challenges 
Battle Pass 
Cosmetics 
Rewards 
Seasonal Progression 
Winstreaks 
Statistics 
Leaderboards 
Player Profile 
The platform must use one coherent event and reward architecture. 
The same gameplay event must update all relevant systems consistently. 
Example: 
A final kill may update: 
Statistics 
Quest Progress 
Achievement Progress 
Battle Pass Progress 
Experience 
Coins 
Seasonal Progress 
Winstreak Logic 
Leaderboards 
Reward Summary 
Replay Timeline 
PlaceholderAPI 
Developer Events 
The platform must prevent duplicate progression updates. ------------------------------------------------------------ 
BEDWARS EXPERIENCE ------------------------------------------------------------ 
Implement a complete BedWars experience system. 
Support: 
Experience earned from gameplay 
Experience earned from wins 
Experience earned from kills 
Experience earned from final kills 
Experience earned from bed destruction 
Experience earned from quests 
Experience earned from achievements 
Experience earned from battle pass rewards 
Experience earned from commands 
Experience earned from API calls 
Experience multipliers 
Rank multipliers 
Event multipliers 
Seasonal multipliers 
Party multipliers 
Booster multipliers 
Daily bonuses 
First win bonuses 
Performance bonuses 
Participation rewards 
Custom experience sources 
All experience sources must be configurable. 
The system must prevent duplicate rewards and exploit-based farming. ------------------------------------------------------------ 
LEVEL SYSTEM 
------------------------------------------------------------ 
Implement: 
BedWars Level 
Current Experience 
Experience Required for Next Level 
Total Lifetime Experience 
Level Progress Percentage 
Level Progress Bar 
Level-Up Events 
Level-Up Rewards 
Level-Based Permissions 
Level-Based Unlocks 
Level-Based Cosmetics 
Level-Based Quests 
Level-Based Shop Access 
Level-Based Atlas Access 
Level-Based Matchmaking Rules 
Configurable maximum level 
Unlimited level mode 
Custom level formulas 
Formula preview 
Formula validation 
Level recalculation tools 
Level migration tools 
Admin level adjustment 
Admin experience adjustment 
Level history 
Level-up notifications 
Level-up sounds 
Level-up particles 
Level-up fireworks 
Level-up GUI 
Level rewards summary ------------------------------------------------------------ 
PRESTIGE SYSTEM ------------------------------------------------------------ 
Implement a complete prestige system. 
Support: 
Default prestiges 
Custom prestiges 
Unlimited custom prestige definitions 
Prestige names 
Prestige display formats 
Prestige colors 
RGB colors 
Gradients 
MiniMessage formats 
Prestige icons 
Prestige symbols 
Prestige level ranges 
Prestige rewards 
Prestige permissions 
Prestige cosmetics 
Prestige titles 
Prestige chat formatting 
Prestige tab formatting 
Prestige scoreboard formatting 
Prestige leaderboard formatting 
Seasonal prestiges 
Hidden prestiges 
Staff-only prestiges 
Event prestiges 
Custom prestige formulas 
Prestige editor GUI 
Prestige preview GUI 
Prestige migration 
Prestige PlaceholderAPI values 
Prestige API ------------------------------------------------------------ 
CURRENCY SYSTEM ------------------------------------------------------------ 
Implement a unified virtual currency framework. 
Support: 
BedWars Coins 
Tokens 
Credits 
Event Currency 
Seasonal Currency 
Cosmetic Currency 
Battle Pass Currency 
Custom Currencies 
Vault-backed Currency 
Command-backed Currency 
Database-backed Currency 
Currency balance 
Currency history 
Currency transaction logs 
Currency rewards 
Currency multipliers 
Currency conversion rules 
Currency caps 
Negative-balance prevention 
Admin add 
Admin remove 
Admin set 
Admin reset 
Currency migration 
Currency API 
Currency placeholders 
Currency GUI 
Currency audit trail 
All currency transactions must be atomic and idempotent. ------------------------------------------------------------ 
COSMETICS SYSTEM ------------------------------------------------------------ 
Create a native enterprise-grade cosmetics platform. 
The engine must support at least 300 built-in configurable cosmetic definitions and unlimited 
custom definitions. 
Do not copy proprietary cosmetic assets, names, sounds, textures, messages or 
implementations. 
Provide original sample cosmetics and a framework capable of supporting a large catalogue. ------------------------------------------------------------ 
COSMETIC CATEGORIES ------------------------------------------------------------ 
Support at minimum: 
Kill Effects 
Final Kill Effects 
Victory Dances 
Bed Destroy Effects 
Projectile Trails 
Glyphs 
Death Cries 
Shopkeeper Skins 
Island Toppers 
Sprays 
Kill Messages 
Final Kill Messages 
Bed Break Messages 
Wood Skins 
Bed Skins 
Lobby Gadgets 
Waiting Lobby Cosmetics 
Cage Cosmetics 
Emotes 
Join Messages 
Leave Messages 
Elimination Messages 
Victory Messages 
Respawn Effects 
Spawn Effects 
Level-Up Effects 
Generator Effects 
Shop Purchase Effects 
Team Introduction Effects 
Bridge Effects 
Footstep Trails 
Arrow Trails 
Fireball Trails 
TNT Effects 
Dragon Effects 
Spectator Effects 
Profile Frames 
Titles 
Badges 
Chat Cosmetics 
Nameplate Cosmetics 
Scoreboard Cosmetics 
Tablist Cosmetics 
Custom Cosmetic Categories ------------------------------------------------------------ 
COSMETIC DEFINITIONS ------------------------------------------------------------ 
Every cosmetic must support: 
Unique Internal ID 
Editable Display Name 
Description 
Category 
Rarity 
Icon 
Material 
Custom Model Data 
Preview 
Unlock Conditions 
Permission Requirement 
Currency Price 
Quest Requirement 
Achievement Requirement 
Level Requirement 
Prestige Requirement 
Battle Pass Requirement 
Season Requirement 
Event Requirement 
Availability Dates 
Limited Availability 
Hidden State 
Staff-Only State 
Default State 
Random Selection Eligibility 
Sound 
Particles 
Animations 
Commands 
Custom Actions 
API Hooks 
Localization Keys 
Database Persistence ------------------------------------------------------------ 
COSMETIC RARITIES ------------------------------------------------------------ 
Support: 
Common 
Uncommon 
Rare 
Epic 
Legendary 
Mythic 
Special 
Seasonal 
Event 
Exclusive 
Custom Rarities 
Every rarity must support: 
Display Name 
Color 
Gradient 
Icon 
Sort Priority 
Unlock Rules 
Price Multiplier 
Animation 
Sound 
GUI Styling ------------------------------------------------------------ 
COSMETIC OWNERSHIP 
------------------------------------------------------------ 
Support: 
Owned Cosmetics 
Temporary Cosmetics 
Trial Cosmetics 
Rental Cosmetics 
Seasonal Cosmetics 
Expired Cosmetics 
Permission Cosmetics 
Rank Cosmetics 
Gifted Cosmetics 
Reward Cosmetics 
Purchased Cosmetics 
Admin-Granted Cosmetics 
Imported Cosmetics 
Ownership History 
Acquisition Source 
Acquisition Timestamp 
Expiration Timestamp ------------------------------------------------------------ 
COSMETIC EQUIPMENT ------------------------------------------------------------ 
Support: 
Equip 
Unequip 
Random Cosmetic 
Random by Category 
Favorite Cosmetics 
Multiple Presets 
Per-Mode Presets 
Per-Arena-Group Presets 
Per-Season Presets 
Preview Before Equip 
Permission Validation 
Ownership Validation 
Fallback Cosmetics 
Disabled Cosmetic Handling 
Invalid Cosmetic Recovery ------------------------------------------------------------ 
COSMETIC GUI ------------------------------------------------------------ 
Provide: 
Main Cosmetics Menu 
Category Menu 
Owned Cosmetics Menu 
Locked Cosmetics Menu 
Favorite Cosmetics Menu 
Recent Cosmetics Menu 
Preset Menu 
Preview Menu 
Purchase Menu 
Confirmation Menu 
Rarity Filter 
Ownership Filter 
Season Filter 
Search 
Pagination 
Sorting 
Admin Editor 
Bulk Grant GUI 
Bulk Revoke GUI 
Player Cosmetic Inspector ------------------------------------------------------------ 
COSMETIC PERFORMANCE ------------------------------------------------------------ 
Cosmetic effects must not significantly affect TPS, memory or network traffic. 
Implement: 
Effect rate limits 
Particle limits 
Entity limits 
Packet limits 
Distance-based visibility 
World-based visibility 
Arena-based visibility 
Spectator visibility rules 
Low-performance mode 
Per-player cosmetic visibility settings 
Global emergency cosmetic disable 
Performance metrics 
Cosmetic profiling ------------------------------------------------------------ 
COSMETIC API ------------------------------------------------------------ 
Provide APIs for: 
Register Cosmetic Category 
Register Cosmetic 
Register Rarity 
Grant Cosmetic 
Revoke Cosmetic 
Equip Cosmetic 
Unequip Cosmetic 
Check Ownership 
Preview Cosmetic 
Create Custom Effects 
Listen to Cosmetic Events 
Query Cosmetic Metadata 
Query Acquisition History 
Create Cosmetic Providers ------------------------------------------------------------ 
QUEST SYSTEM ------------------------------------------------------------ 
Create a complete configurable quest platform. 
Support: 
Daily Quests 
Weekly Quests 
Monthly Quests 
Seasonal Quests 
Event Quests 
One-Time Quests 
Repeatable Quests 
Hidden Quests 
Challenge Quests 
Party Quests 
Team Quests 
Personal Quests 
Community Quests 
Global Server Quests 
Quest Chains 
Quest Branches 
Quest Categories 
Quest Tiers 
Quest Difficulty 
Quest Prerequisites 
Quest Dependencies 
Quest Cooldowns 
Quest Expiration 
Quest Reset 
Quest Reroll 
Quest Abandon 
Quest Pinning 
Quest Tracking 
Quest Notifications 
Quest History 
Quest Failure 
Quest Completion 
Quest Claiming 
Auto-Claim Option 
Manual Claim Option ------------------------------------------------------------ 
QUEST OBJECTIVES ------------------------------------------------------------ 
Support at minimum: 
Play Games 
Win Games 
Lose Games 
Get Kills 
Get Final Kills 
Get Assists 
Break Beds 
Lose Beds 
Place Blocks 
Break Blocks 
Purchase Items 
Purchase Upgrades 
Trigger Traps 
Collect Iron 
Collect Gold 
Collect Diamonds 
Collect Emeralds 
Collect Custom Resources 
Use Specific Items 
Use Specific Shop Categories 
Use Fireballs 
Use TNT 
Use Bridge Eggs 
Use Pop-up Towers 
Use Potions 
Summon Entities 
Destroy Entities 
Survive for Time 
Win Within Time 
Win Without Losing Bed 
Win Without Dying 
Win Without Final Death 
Win with Specific Team Size 
Win in Specific Mode 
Win on Specific Map 
Win in Specific Arena Group 
Win with Specific Party Size 
Reach Winstreak 
Reach Level 
Reach Prestige 
Earn Experience 
Earn Currency 
Complete Other Quests 
Complete Achievements 
Review Atlas Cases 
Receive Accurate Atlas Verdicts 
Watch Replays 
Play Private Games 
Custom Objective API 
Composite Objectives 
AND Objectives 
OR Objectives 
Sequential Objectives 
Timed Objectives ------------------------------------------------------------ 
QUEST PROGRESS ------------------------------------------------------------ 
Quest progress must support: 
Integer Progress 
Decimal Progress 
Duration Progress 
Boolean Progress 
List Progress 
Per-Game Progress 
Lifetime Progress 
Team-Shared Progress 
Party-Shared Progress 
Server-Shared Progress 
Cross-Server Progress 
Atomic Updates 
Duplicate Event Protection 
Rollback Protection 
Offline Progress where valid 
Progress Migration 
Progress Reset 
Progress Import 
Progress Export ------------------------------------------------------------ 
QUEST REWARDS ------------------------------------------------------------ 
Support: 
Experience 
Coins 
Custom Currency 
Cosmetics 
Permissions 
Commands 
Items 
Titles 
Badges 
Battle Pass Experience 
Achievement Points 
Quest Tokens 
Loot Containers 
Temporary Boosters 
Permanent Boosters 
Level Rewards 
Prestige Rewards 
Custom API Rewards 
Multiple Rewards 
Random Rewards 
Weighted Rewards 
Choice Rewards 
Claimable Rewards 
Auto Rewards ------------------------------------------------------------ 
QUEST GUI 
------------------------------------------------------------ 
Provide: 
Quest Overview 
Daily Quests 
Weekly Quests 
Seasonal Quests 
Quest Chains 
Tracked Quests 
Completed Quests 
Claimable Rewards 
Quest History 
Reroll GUI 
Abandon Confirmation 
Reward Preview 
Objective Details 
Progress Bars 
Time Remaining 
Filters 
Sorting 
Search 
Admin Quest Editor 
Quest Template Editor 
Quest Debug GUI 
Player Quest Inspector 
------------------------------------------------------------ 
QUEST ADMINISTRATION ------------------------------------------------------------ 
Commands and GUI must support: 
Create Quest 
Edit Quest 
Duplicate Quest 
Delete Quest 
Enable Quest 
Disable Quest 
Reset Quest 
Complete Quest 
Set Progress 
Add Progress 
Remove Progress 
Reroll Quest 
Assign Quest 
Remove Quest 
Reload Quest Definitions 
Validate Quest 
Import Quest 
Export Quest 
View Quest Logs 
View Player Progress ------------------------------------------------------------ 
QUEST API 
------------------------------------------------------------ 
Provide APIs for: 
Register Objective Type 
Register Reward Type 
Register Quest 
Assign Quest 
Update Progress 
Complete Quest 
Fail Quest 
Claim Reward 
Query Progress 
Query Quest History 
Listen to Quest Events 
Create Quest Providers ------------------------------------------------------------ 
ACHIEVEMENT SYSTEM ------------------------------------------------------------ 
Create a complete achievement platform. 
Support: 
Achievement Categories 
Tiered Achievements 
One-Time Achievements 
Repeatable Achievements 
Hidden Achievements 
Secret Achievements 
Seasonal Achievements 
Event Achievements 
Mode-Specific Achievements 
Map-Specific Achievements 
Arena-Group Achievements 
Team Achievements 
Party Achievements 
Community Achievements 
Staff Achievements 
Custom Achievements 
Achievement Chains 
Achievement Prerequisites 
Achievement Points 
Achievement Completion Dates 
Achievement Progress 
Achievement History ------------------------------------------------------------ 
ACHIEVEMENT OBJECTIVES ------------------------------------------------------------ 
Achievements must support the same objective framework used by quests where 
appropriate. 
The system must avoid duplicating objective logic. 
Achievement-specific objective combinations must be supported. ------------------------------------------------------------ 
ACHIEVEMENT REWARDS ------------------------------------------------------------ 
Support: 
Achievement Points 
Experience 
Coins 
Currency 
Cosmetics 
Titles 
Badges 
Permissions 
Commands 
Items 
Battle Pass Experience 
Permanent Unlocks 
Custom API Rewards ------------------------------------------------------------ 
ACHIEVEMENT GUI ------------------------------------------------------------ 
Provide: 
Achievement Overview 
Category View 
Tier View 
Completed Achievements 
Incomplete Achievements 
Hidden Achievement Rules 
Progress View 
Reward View 
Achievement Points 
Completion Percentage 
Search 
Filters 
Sorting 
Admin Editor 
Player Inspector ------------------------------------------------------------ 
ACHIEVEMENT NOTIFICATIONS ------------------------------------------------------------ 
Support: 
Chat Message 
Title 
Subtitle 
Action Bar 
Boss Bar 
Sound 
Particles 
Fireworks 
Toast-Style Notification 
Discord Notification 
Staff Notification 
Custom Notification Providers ------------------------------------------------------------ 
ACHIEVEMENT API ------------------------------------------------------------ 
Provide APIs for: 
Register Achievement 
Register Category 
Update Progress 
Complete Achievement 
Query Completion 
Query Progress 
Query Points 
Grant Achievement 
Revoke Achievement 
Listen to Events 
Create Objective Providers 
Create Reward Providers ------------------------------------------------------------ 
CHALLENGE SYSTEM ------------------------------------------------------------ 
Implement a challenge system separate from standard quests where appropriate. 
Support: 
Daily Challenges 
Weekly Challenges 
Season Challenges 
Rotating Challenges 
Hardcore Challenges 
Mode Challenges 
Private Game Challenges 
Party Challenges 
Community Challenges 
Streak Challenges 
Time-Limited Challenges 
Challenge Modifiers 
Challenge Rewards 
Challenge Leaderboards 
Challenge History 
Challenge GUI 
Challenge API ------------------------------------------------------------ 
BATTLE PASS SYSTEM ------------------------------------------------------------ 
Create a complete seasonal battle pass platform. 
Support: 
Free Track 
Premium Track 
Multiple Tracks 
Configurable Seasons 
Season Start 
Season End 
Grace Period 
Season Archive 
Season Migration 
Season Reset 
Season Extension 
Battle Pass Levels 
Battle Pass Experience 
Battle Pass Tiers 
Battle Pass Rewards 
Battle Pass Missions 
Battle Pass Challenges 
Battle Pass Boosters 
Battle Pass Multipliers 
Battle Pass Catch-Up Mechanics 
Battle Pass Premium Unlock 
Permission-Based Premium Access 
Currency-Based Premium Access 
External Store Integration Hooks 
Gift Premium Access 
Revoke Premium Access ------------------------------------------------------------ 
BATTLE PASS REWARD TYPES ------------------------------------------------------------ 
Support: 
Coins 
Experience 
Currency 
Cosmetics 
Titles 
Badges 
Permissions 
Commands 
Items 
Loot Containers 
Boosters 
Quest Rerolls 
Exclusive Cosmetics 
Seasonal Cosmetics 
Temporary Rewards 
Permanent Rewards 
Choice Rewards 
Random Rewards 
Custom API Rewards ------------------------------------------------------------ 
BATTLE PASS GUI ------------------------------------------------------------ 
Provide: 
Season Overview 
Free Track View 
Premium Track View 
Current Tier 
Tier Progress 
Reward Preview 
Claim Reward 
Claim All 
Locked Reward View 
Mission View 
Challenge View 
Season Time Remaining 
Previous Season Archive 
Premium Upgrade GUI 
Admin Editor 
Season Editor 
Reward Editor 
Mission Editor 
Player Inspector ------------------------------------------------------------ 
BATTLE PASS ADMINISTRATION ------------------------------------------------------------ 
Support: 
Create Season 
Duplicate Season 
Edit Season 
Delete Season 
Enable Season 
Disable Season 
Start Season 
End Season 
Extend Season 
Reset Season 
Archive Season 
Import Season 
Export Season 
Grant Premium 
Revoke Premium 
Set Tier 
Set Experience 
Add Experience 
Remove Experience 
Claim Reward 
Reset Player Progress 
Validate Season 
Reload Battle Pass ------------------------------------------------------------ 
BATTLE PASS API ------------------------------------------------------------ 
Provide APIs for: 
Register Season 
Register Track 
Register Reward 
Register Mission 
Add Experience 
Set Experience 
Grant Premium 
Revoke Premium 
Claim Reward 
Query Progress 
Query Premium State 
Listen to Battle Pass Events ------------------------------------------------------------ 
REWARD ENGINE ------------------------------------------------------------ 
Create one unified reward engine used by: 
Games 
Quests 
Achievements 
Battle Pass 
Leveling 
Prestiges 
Seasonal Events 
Holiday Rewards 
Admin Rewards 
Reward Commands 
Promotions 
Daily Rewards 
Weekly Rewards 
Community Rewards 
The reward engine must support: 
Idempotent rewards 
Transactional rewards 
Rollback 
Retry 
Failure queue 
Audit log 
Reward preview 
Reward claim state 
Expiration 
Delayed delivery 
Offline delivery 
Cross-server delivery 
Duplicate prevention ------------------------------------------------------------ 
REWARD SUMMARY ------------------------------------------------------------ 
After each game, provide a configurable reward summary including: 
Experience earned 
Coins earned 
Currency earned 
Quest progress 
Completed quests 
Achievement progress 
Completed achievements 
Battle Pass experience 
Battle Pass tiers gained 
Level progress 
Level gained 
Prestige progress 
Winstreak change 
Cosmetics unlocked 
Other rewards 
The summary must support: 
Chat 
GUI 
Title 
Boss Bar 
Action Bar 
Discord integration 
PlaceholderAPI 
API access ------------------------------------------------------------ 
HOLIDAY AND EVENT REWARDS ------------------------------------------------------------ 
Implement: 
Holiday Rewards 
Seasonal Rewards 
Anniversary Rewards 
Login Rewards 
First Win Rewards 
Weekend Rewards 
Server Event Rewards 
Custom Calendar Events 
Time Zone Configuration 
Reward Windows 
Claim Limits 
Global Claim Limits 
Per-Player Claim Limits 
Permission Requirements 
Level Requirements 
Game Requirements ------------------------------------------------------------ 
PLAYER PROFILE ------------------------------------------------------------ 
Create a complete player profile system. 
Include: 
Level 
Prestige 
Experience 
Currencies 
Statistics Summary 
Winstreak 
Best Winstreak 
Cosmetics 
Quests 
Achievements 
Battle Pass 
Titles 
Badges 
Recent Games 
Recent Rewards 
Replay History 
Atlas Reviewer Status 
Party Information 
Language 
Settings 
Privacy Settings 
Profile Visibility 
Favorite Maps 
Favorite Modes 
Favorite Cosmetics 
Profile GUI 
Profile API 
Profile Placeholders ------------------------------------------------------------ 
PROGRESSION SETTINGS ------------------------------------------------------------ 
Players must be able to configure: 
Reward Summary Visibility 
Quest Tracking 
Achievement Notifications 
Cosmetic Visibility 
Battle Pass Notifications 
Level-Up Notifications 
Sound Preferences 
Particle Preferences 
Profile Privacy 
Leaderboard Visibility 
Replay History Visibility 
Atlas History Visibility ------------------------------------------------------------ 
PROGRESSION DATABASE ------------------------------------------------------------ 
All progression data must support: 
SQLite 
MySQL 
MariaDB 
HikariCP 
Caching 
Async Writes 
Atomic Transactions 
Schema Versioning 
Migration 
Backup 
Restore 
Export 
Import 
Conflict Resolution 
Cross-Server Synchronization 
Redis Cache Invalidation 
Data Repair 
Admin Inspection 
GDPR-Style Deletion ------------------------------------------------------------ 
PROGRESSION PLACEHOLDERS ------------------------------------------------------------ 
Every meaningful value must be available through PlaceholderAPI. 
Include at minimum: 
Current Experience 
Required Experience 
Experience Progress 
Level 
Prestige 
Prestige Format 
Coins 
Every Custom Currency 
Quest Progress 
Quest Completion 
Tracked Quest 
Achievement Progress 
Achievement Points 
Battle Pass Tier 
Battle Pass Experience 
Premium State 
Cosmetic Count 
Equipped Cosmetics 
Unlocked Titles 
Unlocked Badges 
Reward Multipliers 
Season Information 
Challenge Progress 
Daily Progress 
Weekly Progress 
Lifetime Progress ------------------------------------------------------------ 
PROGRESSION COMMANDS ------------------------------------------------------------ 
Provide complete commands for: 
Profile 
Level 
Experience 
Prestige 
Coins 
Currencies 
Cosmetics 
Quests 
Achievements 
Challenges 
Battle Pass 
Rewards 
Titles 
Badges 
Admin Progression 
Admin Currency 
Admin Cosmetics 
Admin Quests 
Admin Achievements 
Admin Battle Pass 
Admin Rewards 
Admin Data Repair 
Admin Migration 
Every command must include: 
Permissions 
Tab Completion 
Localization 
Validation 
Audit Logging 
Documentation ------------------------------------------------------------ 
PROGRESSION PERMISSIONS ------------------------------------------------------------ 
Create granular permissions for: 
View Own Data 
View Other Player Data 
Use Cosmetics 
Use Category 
Equip Cosmetic 
Preview Cosmetic 
Purchase Cosmetic 
Bypass Requirements 
Use Quests 
Reroll Quests 
Use Battle Pass 
Use Premium Track 
Claim Rewards 
Use Profile 
Edit Privacy 
Admin View 
Admin Edit 
Admin Grant 
Admin Revoke 
Admin Reset 
Admin Import 
Admin Export 
Admin Debug 
Admin Bypass ------------------------------------------------------------ 
ADMIN EDITORS ------------------------------------------------------------ 
Provide in-game editors for: 
Cosmetics 
Rarities 
Quests 
Quest Objectives 
Quest Rewards 
Achievements 
Achievement Rewards 
Challenges 
Battle Pass Seasons 
Battle Pass Tracks 
Battle Pass Rewards 
Level Formulas 
Prestiges 
Currencies 
Reward Definitions 
Holiday Rewards 
All editors must include: 
Validation 
Preview 
Duplicate 
Import 
Export 
Undo where practical 
Confirmation 
Permission Checks 
Audit Logging ------------------------------------------------------------ 
MIGRATION ------------------------------------------------------------ 
Provide migration support where technically and legally appropriate for: 
Existing BedWars statistics 
Existing level data 
Existing currency data 
Existing cosmetics ownership 
Existing quest data 
Existing achievement data 
Existing BedWars1058-compatible layouts or player data 
Migration must never copy proprietary code or assets. 
Migration must include: 
Dry Run 
Validation 
Backup 
Progress Display 
Error Report 
Rollback 
Duplicate Detection 
ID Mapping ------------------------------------------------------------ 
PERFORMANCE REQUIREMENTS ------------------------------------------------------------ 
Progression systems must be designed for high event volume. 
Avoid: 
Database write per event 
Full player-data reloads 
Repeated PlaceholderAPI database queries 
Unbounded history retention 
Synchronous reward processing 
Synchronous cosmetic asset loading 
Use: 
Event aggregation 
Write-behind caching 
Batching 
Async persistence 
Bounded queues 
Cached placeholders 
Precompiled conditions 
Indexed database queries 
Lazy GUI loading 
Pagination ------------------------------------------------------------ 
SECURITY AND EXPLOIT PREVENTION ------------------------------------------------------------ 
Implement protection against: 
Duplicate rewards 
Quest farming exploits 
Repeated event submission 
Currency duplication 
Battle Pass double claims 
Cosmetic ownership spoofing 
Permission bypass 
Replay reward farming 
Atlas reward farming 
Cross-server desynchronization 
Database race conditions 
Command abuse 
Admin action abuse 
Invalid configuration injection 
All sensitive actions must be auditable. ------------------------------------------------------------ 
GENERAL COMPLETION RULE ------------------------------------------------------------ 
Every system in Part 4C is mandatory. 
A progression feature is complete only when it includes: 
Core logic 
Persistent data 
Configuration 
GUI 
Commands 
Permissions 
Localization 
PlaceholderAPI 
Public API 
Events 
Documentation 
Migration where relevant 
Automated tests 
Performance validation 
Security validation 
Administrative tooling 
No subsystem may be marked complete before all applicable requirements are satisfied. ------------------------------------------------------------ 
MANDATORY FUNCTIONAL SCOPE 
PART 4D ------------------------------------------------------------ 
The following systems are mandatory: 
Replay System 
Match Recording 
Staff Investigation 
Atlas-Style Community Review 
Report Integration 
Anti-Cheat Evidence Integration 
Statistics 
Ratios 
Winstreaks 
Leaderboards 
PlaceholderAPI 
Discord Statistics Integration 
Every system must be production-ready. 
No subsystem may be represented by: 
Placeholder menus 
Incomplete commands 
Mock data 
Sample-only code 
Future implementation notes 
TODO items 
Every applicable system must include: 
Core Logic 
Persistent Storage 
Configuration 
GUI 
Commands 
Permissions 
Localization 
Public API 
Events 
PlaceholderAPI 
Documentation 
Automated Tests 
Performance Controls 
Security Controls 
Administrative Tools 
Migration Support ------------------------------------------------------------ 
REPLAY PLATFORM ------------------------------------------------------------ 
Create a complete native match replay and evidence platform. 
The replay system must be designed for: 
Normal Player Replay Viewing 
Staff Investigation 
Atlas Community Review 
Anti-Cheat Evidence 
Player Reports 
Tournament Review 
Bug Investigation 
Match History 
Administrative Auditing 
The replay system must not be implemented as a simple video file. 
It must record structured game-state and event data sufficient to reconstruct the relevant 
match experience. ------------------------------------------------------------ 
REPLAY IDENTITY ------------------------------------------------------------ 
Every replay must have: 
Immutable Replay ID 
Match ID 
Arena ID 
Map ID 
Map Display Name Snapshot 
Arena Group ID 
Mode ID 
Server ID 
Backend ID 
Proxy ID where applicable 
Start Timestamp 
End Timestamp 
Duration 
Participating Player UUIDs 
Team Assignments 
Party Metadata where permitted 
Game Result 
Winning Team 
Replay Version 
Data Format Version 
Recording Status 
Integrity Status 
Retention Status 
Case References 
Report References 
Anti-Cheat References 
Atlas References 
Every replay identifier must be unique and collision-resistant. 
Replay identity must not depend on map display name, player name or server display name. ------------------------------------------------------------ 
MATCH RECORDING ------------------------------------------------------------ 
Record enough information to reconstruct the match accurately. 
Record at minimum: 
Player UUID 
Player Display Name Snapshot 
Player Skin Reference 
Team 
Position 
Rotation 
Velocity 
Movement State 
Sneaking 
Sprinting 
Jumping 
Flying State where relevant 
Inventory State where relevant 
Held Item 
Armor 
Health 
Absorption 
Potion Effects 
Death 
Respawn 
Disconnect 
Reconnect 
Bed State 
Team State 
Arena State 
Placed Blocks 
Broken Blocks 
Explosions 
TNT 
Fireballs 
Projectiles 
Projectile Paths 
Entity Spawns 
Entity Deaths 
Shop Purchases 
Upgrade Purchases 
Generator Events 
Resource Collection 
Trap Triggers 
Bed Breaks 
Kills 
Final Kills 
Assists 
Void Deaths 
Game Events 
Chat Metadata where legally and configurably permitted 
Reports 
Anti-Cheat Flags 
Staff Actions 
Atlas Case Markers 
Recording frequency must be configurable and performance-aware. 
The system must support: 
Event-Based Recording 
Snapshot Recording 
Hybrid Recording 
Adaptive Recording Frequency 
Compression 
Delta Encoding 
Batch Writing 
Async Persistence ------------------------------------------------------------ 
REPLAY ACCURACY ------------------------------------------------------------ 
Replay reconstruction must preserve: 
Relative event order 
Match timing 
Player movement timing 
Combat event timing 
Projectile timing 
Bed destruction timing 
Death and respawn timing 
Team elimination timing 
Game phase transitions 
The replay system must clearly identify data that is: 
Exact 
Sampled 
Estimated 
Unavailable 
Derived 
Estimated values such as reach must never be presented as exact values. ------------------------------------------------------------ 
REPLAY VIEWER ------------------------------------------------------------ 
Implement a complete replay viewer. 
Support: 
Open Replay 
Close Replay 
Pause 
Resume 
Restart 
Jump to Start 
Jump to End 
Rewind 
Fast Forward 
Frame Step where technically practical 
Speed Control 
Slow Motion 
Fast Playback 
Timeline Scrubbing 
Free Camera 
First-Person View 
Third-Person View 
Player Follow 
Player Selector 
Team Selector 
Event Selector 
Teleport to Event 
Teleport to Player 
Camera Presets 
Cinematic Camera where practical 
Hide Interface 
Show Interface 
Exit Replay ------------------------------------------------------------ 
REPLAY SPEEDS ------------------------------------------------------------ 
Support configurable replay speeds including: 
0.10x 
0.25x 
0.50x 
0.75x 
1.00x 
1.50x 
2.00x 
4.00x 
Custom Speed 
The system must prevent invalid or unsafe playback speeds. ------------------------------------------------------------ 
REPLAY TIMELINE ------------------------------------------------------------ 
Display a searchable and filterable timeline. 
Timeline event types must include: 
Match Start 
Player Join 
Player Leave 
Reconnect 
Kill 
Final Kill 
Assist 
Death 
Void Death 
Bed Break 
Bed Lost 
Team Elimination 
Purchase 
Upgrade 
Trap 
Generator Upgrade 
Resource Collection 
Special Item Use 
Projectile Hit 
Suspicious Hit 
Anti-Cheat Flag 
Report 
Staff Marker 
Atlas Marker 
Match End 
Custom Developer Events 
Timeline entries must support: 
Timestamp 
Actor 
Target 
Team 
Location 
Event Type 
Metadata 
Severity 
Evidence Link 
Filter 
Search 
Jump to Event ------------------------------------------------------------ 
REPLAY TELEMETRY ------------------------------------------------------------ 
When technically reliable, expose: 
Ping 
Server TPS 
Server MSPT 
Packet Delay 
CPS 
Estimated Reach 
Hit Distance 
Attack Timing 
Rotation Delta 
Yaw 
Pitch 
Velocity 
Knockback 
Movement Speed 
Acceleration 
Ground State 
Air Time 
Bridge Placement Rate 
Block Placement Pattern 
Click Pattern 
Projectile Accuracy 
Combat Target Switching 
Anti-Cheat Violation Level 
Anti-Cheat Check Name 
Data Source 
Confidence Level 
Telemetry must identify whether the value originates from: 
Native Recording 
Grim 
Vulcan 
Another Anti-Cheat Provider 
Packet Analysis 
Derived Calculation 
Staff Annotation ------------------------------------------------------------ 
REPLAY GUI ------------------------------------------------------------ 
Provide: 
Replay Browser 
Recent Replays 
Player Replays 
Map Replays 
Mode Replays 
Reported Replays 
Flagged Replays 
Atlas Replays 
Staff Cases 
Favorite Replays 
Archived Replays 
Replay Search 
Replay Filters 
Replay Details 
Replay Metadata 
Replay Timeline 
Replay Telemetry 
Replay Settings 
Replay Storage Status 
Replay Admin Panel 
Replay GUI must support: 
Pagination 
Search 
Sorting 
Filtering 
Permission Validation 
Confirmation 
Error States 
Loading States 
Progress Indicators ------------------------------------------------------------ 
PLAYER REPLAY ACCESS ------------------------------------------------------------ 
Player access must be configurable. 
Support permissions for: 
View Own Replays 
View Party Replays 
View Public Replays 
View Tournament Replays 
View Specific Modes 
View Specific Arena Groups 
Download Evidence Metadata where permitted 
Favorite Replay 
Share Replay Reference 
View Replay History 
Players must not automatically see: 
Hidden Staff Evidence 
Private Reports 
Real Atlas Suspect Identity 
Internal Anti-Cheat Notes 
Private Staff Annotations 
Restricted Chat Data ------------------------------------------------------------ 
STAFF REPLAY INVESTIGATION ------------------------------------------------------------ 
Staff tools must include: 
Open Suspect Replay 
Auto-Follow Suspect 
View First Person 
View Third Person 
View Free Camera 
View Hitboxes 
View Path 
View Attack Rays where technically appropriate 
View CPS 
View Estimated Reach 
View Rotation Graph 
View Movement Graph 
View Velocity Graph 
View Knockback 
View Ping 
View TPS and MSPT 
View Anti-Cheat Flags 
View Reports 
View Timeline 
Add Annotation 
Add Evidence Marker 
Create Case 
Link Existing Case 
Escalate Case 
Assign Case 
Set Priority 
Set Case Status 
Record Staff Verdict 
Apply Staff Action through permission-controlled integrations 
Export Evidence Summary ------------------------------------------------------------ 
REPLAY ANNOTATIONS ------------------------------------------------------------ 
Support: 
Staff Notes 
Timestamped Notes 
Private Notes 
Shared Staff Notes 
Evidence Markers 
Severity 
Categories 
Attachments or References where supported 
Author 
Creation Timestamp 
Last Modified 
Audit History 
Annotations must be permission-controlled and auditable. ------------------------------------------------------------ 
REPLAY STORAGE ------------------------------------------------------------ 
Support configurable replay storage providers: 
Local Filesystem 
Network Filesystem 
Database Metadata with File Payload 
Object Storage Adapter API 
Custom Storage Provider 
Replay storage must support: 
Compression 
Encryption where configured 
Checksums 
Integrity Validation 
Versioning 
Retention 
Archiving 
Deletion 
Backup 
Restore 
Migration 
Storage Quotas 
Per-Mode Retention 
Per-Case Retention 
Per-Report Retention 
Protected Replays 
Legal Hold Flag 
Automatic Cleanup 
Manual Cleanup 
Storage Health Monitoring ------------------------------------------------------------ 
REPLAY RETENTION ------------------------------------------------------------ 
Support configurable retention rules based on: 
Age 
Storage Size 
Match Type 
Report Status 
Anti-Cheat Flag 
Atlas Case 
Staff Case 
Tournament Status 
Favorite Status 
Protected Status 
Player Rank where appropriate 
Replay retention must never delete: 
Protected Evidence 
Open Staff Cases 
Open Atlas Cases 
Replays under explicit administrative hold 
unless an authorized administrator performs a forced action. ------------------------------------------------------------ 
REPLAY RECOVERY ------------------------------------------------------------ 
Implement: 
Partial Replay Detection 
Interrupted Recording Recovery 
Corrupt Replay Detection 
Checksum Validation 
Repair Attempts 
Quarantine 
Recovery Logs 
Administrative Notification 
The system must not load corrupted replay data silently. ------------------------------------------------------------ 
REPLAY PERFORMANCE ------------------------------------------------------------ 
Replay recording must be designed to minimize: 
Main-Thread Work 
Object Allocation 
Disk I/O Spikes 
Network Traffic 
Memory Retention 
Chunk Loading 
Entity Duplication 
Database Chatter 
Use: 
Async Encoding 
Buffered Writing 
Batching 
Compression 
Delta Frames 
Object Pools where justified 
Bounded Queues 
Backpressure 
Adaptive Sampling 
Per-Server Limits 
Emergency Degradation Mode 
Emergency Recording Disable for non-evidence matches 
Evidence replays must have higher retention and reliability priority than ordinary replays. ------------------------------------------------------------ 
REPLAY PRIVACY ------------------------------------------------------------ 
Replay data must support configurable privacy controls. 
Support: 
Anonymized Player Names 
Hidden UUIDs for non-staff viewers 
Chat Recording Disabled by Default 
Chat Metadata Only 
Full Chat Recording only when explicitly enabled and legally appropriate 
Staff-Only Evidence 
Player History Visibility Settings 
Atlas Identity Protection 
Data Export 
Data Deletion Requests where legally required 
Retention Transparency 
Replay access must be logged when configured. ------------------------------------------------------------ 
REPLAY COMMANDS ------------------------------------------------------------ 
Provide at minimum: 
/replay 
/replay list 
/replay recent 
/replay view <replayId> 
/replay player <player> 
/replay map <map> 
/replay mode <mode> 
/replay favorite <replayId> 
/replay unfavorite <replayId> 
/replay info <replayId> 
/replay report <replayId> 
/replay settings 
/replay admin 
/replay admin search 
/replay admin protect 
/replay admin unprotect 
/replay admin archive 
/replay admin delete 
/replay admin restore 
/replay admin repair 
/replay admin storage 
/replay admin migrate 
/replay admin cleanup 
/replay admin export 
Every command must include: 
Permission 
Validation 
Tab Completion 
Localization 
Audit Logging where sensitive 
Documentation ------------------------------------------------------------ 
REPLAY PERMISSIONS ------------------------------------------------------------ 
Create granular permissions for: 
Use Replay 
View Own Replay 
View Other Replay 
View Public Replay 
View Restricted Replay 
View Staff Evidence 
View Atlas Evidence 
View Real Identity 
View Anti-Cheat Data 
View Reports 
Add Annotation 
Edit Annotation 
Delete Annotation 
Create Case 
Link Case 
Protect Replay 
Archive Replay 
Delete Replay 
Restore Replay 
Export Replay 
Repair Replay 
Manage Storage 
Bypass Retention 
Admin Replay 
------------------------------------------------------------ 
REPLAY API ------------------------------------------------------------ 
Provide APIs for: 
Start Recording 
Stop Recording 
Register Replay Event Type 
Write Custom Event 
Query Replay 
Search Replay 
Open Viewer 
Close Viewer 
Add Annotation 
Add Evidence Marker 
Protect Replay 
Archive Replay 
Delete Replay 
Create Case Reference 
Register Storage Provider 
Register Telemetry Provider 
Listen to Replay Events 
Query Replay Integrity ------------------------------------------------------------ 
ATLAS COMMUNITY REVIEW SYSTEM ------------------------------------------------------------ 
Create an original community replay review system inspired by large-network community 
review concepts. 
The system must allow qualified players to review anonymized replay cases and submit 
evidence-based verdicts. 
Community verdicts must not directly trigger permanent punishment unless explicitly 
approved by a separate staff-controlled policy. 
Default behavior: 
Community Review 
Reviewer Reputation Evaluation 
Staff Final Decision ------------------------------------------------------------ 
ATLAS ACCESS REQUIREMENTS ------------------------------------------------------------ 
Default access requirements: 
Minimum BedWars Level: 20 
Minimum Games Played: 100 
Both Requirements Required: true 
All values must be configurable. 
Access methods must include: 
Meeting Configured Requirements 
Staff Permission 
Administrator Permission 
VIP Bypass Permission 
Dedicated Bypass Permission 
Temporary Reviewer Permission 
Manual Reviewer Approval 
Reviewer Whitelist 
Reviewer access must never rely on a hardcoded rank name. ------------------------------------------------------------ 
ATLAS PERMISSIONS ------------------------------------------------------------ 
Provide granular permissions including: 
zartrabedwars.atlas.use 
zartrabedwars.atlas.review 
zartrabedwars.atlas.bypass.requirements 
zartrabedwars.atlas.vip 
zartrabedwars.atlas.staff 
zartrabedwars.atlas.admin 
zartrabedwars.atlas.view.history 
zartrabedwars.atlas.view.stats 
zartrabedwars.atlas.view.leaderboard 
zartrabedwars.atlas.view.identity 
zartrabedwars.atlas.manage.cases 
zartrabedwars.atlas.manage.reviewers 
zartrabedwars.atlas.suspend.reviewer 
zartrabedwars.atlas.unsuspend.reviewer 
zartrabedwars.atlas.override.verdict 
zartrabedwars.atlas.reward 
zartrabedwars.atlas.debug 
Permission names may follow the final project namespace convention but must remain 
granular. ------------------------------------------------------------ 
ATLAS CASE CREATION 
------------------------------------------------------------ 
Cases may be created from: 
Player Reports 
Grim Alerts 
Vulcan Alerts 
Internal Detection 
Staff Manual Creation 
Replay Evidence 
Automated Suspicion Thresholds 
Multiple Reports 
Cross-Teaming Reports 
Boosting Reports 
Exploiting Reports 
Cases must contain: 
Case ID 
Replay ID 
Suspect Anonymous Alias 
Real Suspect UUID Staff-Only 
Game Mode 
Map Snapshot 
Case Category 
Creation Source 
Creation Timestamp 
Priority 
Evidence Markers 
Anti-Cheat Alerts 
Reports 
Review Status 
Reviewer Count 
Verdict Distribution 
Staff Status 
Final Staff Decision 
Case Integrity State ------------------------------------------------------------ 
ATLAS ANONYMIZATION ------------------------------------------------------------ 
Community reviewers must not see: 
Real Suspect Name 
Real Suspect UUID 
Rank 
Guild 
Party Identity 
Friends 
Personal Profile 
Known Punishment History 
Reporter Identity 
Staff Notes 
Any metadata likely to bias the reviewer 
Use randomized or deterministic anonymous aliases such as: 
Suspect 
Player A 
Player B 
Red Player 
Blue Player 
Aliases must remain consistent inside one case. 
Only authorized staff may reveal real identities. ------------------------------------------------------------ 
ATLAS REVIEW FLOW ------------------------------------------------------------ 
The review flow must include: 
Open Atlas 
Atlas Menu 
Check Reviewer Eligibility 
Display Reviewer Requirements 
Reserve Available Case 
Load Anonymized Replay 
Show Review Instructions 
Display Case Category 
Display Relevant Evidence Markers 
Allow Full Replay Controls 
Allow Timeline Navigation 
Allow Player Switching 
Allow First-Person Viewing 
Allow Third-Person Viewing 
Allow Free Camera 
Allow Slow Motion 
Allow Repeat Viewing 
Allow Telemetry Inspection 
Allow Anti-Cheat Alert Inspection where permitted 
Allow Evidence Marker Inspection 
Allow Case Skip 
Allow Verdict Selection 
Allow Optional or Required Reason 
Require Verdict Confirmation 
Submit Review 
Record Review Duration 
Record Replay Interaction Time 
Record Viewed Timeline Sections 
Update Reviewer Statistics 
Update Reviewer Reputation 
Release Reserved Case 
Prevent Duplicate Review by the Same Reviewer 
Prevent Reviewing Own Case 
Prevent Reviewing Cases Involving Current Party Members 
Prevent Reviewing Cases Involving Known Conflicts where detectable 
Prevent Multiple Concurrent Case Reservations 
Automatically Release Abandoned Cases 
Handle Replay Loading Failure 
Handle Corrupted Replay 
Handle Reviewer Disconnect 
Restore Review Session where safe ------------------------------------------------------------ 
ATLAS VERDICTS ------------------------------------------------------------ 
Support at minimum: 
Evidently Cheating 
Insufficient Evidence 
Not Cheating 
Cross Teaming 
Boosting 
Exploiting 
Abusing Bugs 
Other Rule Violation 
Invalid Replay 
Corrupted Replay 
Unable to Review 
Skip Case 
Every verdict must support: 
Unique Internal ID 
Editable Display Name 
Description 
Icon 
Color 
Material 
Custom Model Data 
Permission Requirement 
Supported Case Categories 
Reason Requirement 
Minimum Review Time 
Reward Eligibility 
Staff Verdict Mapping 
Atlas Reputation Impact 
Localization 
API Access 
PlaceholderAPI Access 
Administrators must be able to create custom verdict types. ------------------------------------------------------------ 
ATLAS REVIEW REASONS ------------------------------------------------------------ 
Support configurable reasons such as: 
KillAura 
Reach 
Velocity 
Anti-Knockback 
AutoClicker 
AimAssist 
Scaffold 
Speed 
Fly 
Movement Modification 
Impossible Rotation 
Suspicious CPS 
Cross Teaming 
Boosting 
Bug Abuse 
Game Exploit 
Insufficient Evidence 
Replay Error 
Other 
Each reason must support: 
Internal ID 
Display Name 
Description 
Category 
Icon 
Permission 
Required Verdict 
Optional Notes 
Localization 
Staff Mapping ------------------------------------------------------------ 
ATLAS REVIEWER PROFILE ------------------------------------------------------------ 
Track: 
Reviewer UUID 
Reviewer Status 
Reviewer Tier 
Reviewer Reputation 
Reviewer Accuracy 
Lifetime Cases Reviewed 
Daily Cases Reviewed 
Weekly Cases Reviewed 
Monthly Cases Reviewed 
Accurate Verdicts 
Inaccurate Verdicts 
Neutral Verdicts 
Skipped Cases 
Invalid Reviews 
Suspended Reviews 
Average Review Duration 
Average Replay Watch Percentage 
Average Timeline Interaction 
Recent Accuracy 
Lifetime Accuracy 
Category Accuracy 
Cheating Detection Accuracy 
Cross Teaming Accuracy 
Boosting Accuracy 
Exploit Accuracy 
Current Review Streak 
Best Review Streak 
Rewards Earned 
Reviewer Warnings 
Reviewer Suspensions 
Suspension Reason 
Suspension Expiration 
Last Review Time 
Reviewer Join Date ------------------------------------------------------------ 
ATLAS REPUTATION SYSTEM ------------------------------------------------------------ 
Implement a configurable reputation system. 
Reputation may increase through: 
Accurate Staff-Confirmed Verdicts 
Accurate Consensus Verdicts where policy allows 
Long-Term Reviewer Accuracy 
High-Quality Reasons 
Evidence Marker Usage 
Consistent Review Duration 
Successful Category Specialization 
Daily Review Goals 
Weekly Review Goals 
Staff Commendation 
Reputation may decrease through: 
Random Voting 
Repeated Incorrect Verdicts 
Suspiciously Fast Reviews 
Review Farming 
Collusion 
Conflicted Case Reviews 
Invalid Reasons 
Repeated Case Abandonment 
Confirmed Abuse 
Staff Penalty 
Support reviewer tiers such as: 
Trainee Reviewer 
Reviewer 
Trusted Reviewer 
Senior Reviewer 
Expert Reviewer 
Custom Reviewer Tiers 
Every tier must support: 
Minimum Reputation 
Minimum Accuracy 
Minimum Cases Reviewed 
Permission Grants 
Reward Multipliers 
Case Priority 
Case Category Access 
Display Format 
Badge 
Title 
GUI Styling ------------------------------------------------------------ 
ATLAS ACCURACY CALCULATION ------------------------------------------------------------ 
Reviewer accuracy must be calculated primarily from cases with trusted final outcomes. 
Trusted outcomes may include: 
Staff Final Verdict 
Confirmed Ban Outcome 
Approved Appeal Outcome 
Verified Anti-Cheat Evidence 
Manual Administrative Review 
Approved Consensus Threshold 
Tournament Staff Decision 
Accuracy formulas must be configurable. 
Support: 
Lifetime Accuracy 
Recent Accuracy 
Category Accuracy 
Weighted Accuracy 
Confidence-Adjusted Accuracy 
Minimum Sample Size 
Decay of Old Reviews 
Staff-Weighted Outcomes 
Do not treat community majority alone as absolute truth by default. ------------------------------------------------------------ 
ATLAS ANTI-ABUSE SYSTEM ------------------------------------------------------------ 
Detect and prevent: 
Random Voting 
Instant Voting 
Voting Without Watching Replay 
Repeated Identical Verdict Patterns 
Automated Macro Reviewing 
Multi-Account Review Farming 
Reward Farming 
Collusion 
Targeted Voting 
Reviewer Brigading 
Reviewing Own Case 
Reviewing Party Member Cases 
Reviewing Friend Cases where detectable 
Reviewing Guild Member Cases where configured 
Repeated Conflict-of-Interest Cases 
Suspiciously Short Review Duration 
Repeated Skipping 
Repeated Corrupted Replay Claims 
Unusual Accuracy Manipulation 
VPN or Shared-Environment Abuse where lawful and appropriate 
The anti-abuse system must support: 
Warnings 
Temporary Review Lock 
Temporary Suspension 
Permanent Reviewer Removal 
Reward Removal 
Reputation Reduction 
Review Invalidation 
Case Reassignment 
Manual Staff Investigation 
Audit Logs 
Appeal System 
False-Positive Review ------------------------------------------------------------ 
ATLAS REVIEW INTERACTION REQUIREMENTS ------------------------------------------------------------ 
The system may require configurable interactions before verdict submission. 
Examples: 
Minimum Replay Watch Time 
Minimum Percentage Watched 
At Least One Timeline Interaction 
At Least One Player Perspective Change 
At Least One Evidence Marker Opened 
At Least One Replay Pause 
At Least One Speed Change 
These controls must be configurable and must not create meaningless forced actions. ------------------------------------------------------------ 
ATLAS REWARDS ------------------------------------------------------------ 
Support rewards for: 
Completed Review 
Accurate Review 
High-Quality Review 
Accurate Review Streak 
Daily Review Goal 
Weekly Review Goal 
Monthly Review Goal 
Reviewer Tier Promotion 
Category Expertise 
Staff Commendation 
Rewards may include: 
BedWars Experience 
Coins 
Custom Currency 
Atlas Reputation 
Atlas Tokens 
Cosmetics 
Titles 
Badges 
Permissions 
Commands 
Battle Pass Experience 
Quest Progress 
Achievement Progress 
Temporary Boosters 
Permanent Unlocks 
Rewards must be transactional and duplicate-protected. 
The system must not reward low-quality rapid voting. ------------------------------------------------------------ 
ATLAS QUESTS AND ACHIEVEMENTS ------------------------------------------------------------ 
Support: 
Atlas Daily Quests 
Atlas Weekly Quests 
Accurate Review Quests 
Reviewer Streak Quests 
Category Review Quests 
Trusted Reviewer Achievements 
Accuracy Achievements 
Case Milestone Achievements 
Atlas rewards must integrate with the shared quest, achievement and reward engines. ------------------------------------------------------------ 
ATLAS GUI ------------------------------------------------------------ 
Provide: 
Atlas Main Menu 
Eligibility Status 
Reviewer Requirements 
Reviewer Profile 
Reviewer Statistics 
Reviewer Accuracy 
Reviewer Reputation 
Reviewer Tier 
Reviewer Rewards 
Reviewer History 
Reviewer Leaderboard 
Case Queue 
Reserved Case 
Review Instructions 
Replay Controls 
Timeline 
Evidence Markers 
Telemetry 
Verdict Selection 
Reason Selection 
Verdict Confirmation 
Case Skip Confirmation 
Suspension Status 
Reviewer Settings 
Staff Case Queue 
Staff Case Details 
Staff Final Verdict 
Reviewer Management 
Atlas Configuration 
Atlas Diagnostics 
Atlas Performance Dashboard 
Atlas Audit Log 
All Atlas GUIs must support: 
Pagination 
Search 
Filters 
Sorting 
Loading States 
Error States 
Permissions 
Localization 
Confirmation ------------------------------------------------------------ 
ATLAS COMMANDS ------------------------------------------------------------ 
Provide at minimum: 
/atlas 
/atlas review 
/atlas eligibility 
/atlas stats 
/atlas profile 
/atlas history 
/atlas leaderboard 
/atlas rewards 
/atlas skip 
/atlas report <player> 
/atlas settings 
/atlas help 
/atlas admin 
/atlas admin cases 
/atlas admin case <caseId> 
/atlas admin create 
/atlas admin assign 
/atlas admin unassign 
/atlas admin close 
/atlas admin reopen 
/atlas admin delete 
/atlas admin reviewer <player> 
/atlas admin approve <player> 
/atlas admin suspend <player> 
/atlas admin unsuspend <player> 
/atlas admin warn <player> 
/atlas admin reputation <player> 
/atlas admin invalidate <reviewId> 
/atlas admin override <caseId> 
/atlas admin reveal <caseId> 
/atlas admin config 
/atlas admin reload 
/atlas admin diagnostics 
/atlas admin performance 
Every command must include: 
Permission 
Tab Completion 
Localization 
Validation 
Usage 
Examples 
Audit Logging where sensitive ------------------------------------------------------------ 
ATLAS PERMISSIONS ------------------------------------------------------------ 
Provide at minimum: 
zartrabedwars.atlas.use 
zartrabedwars.atlas.review 
zartrabedwars.atlas.skip 
zartrabedwars.atlas.stats 
zartrabedwars.atlas.profile 
zartrabedwars.atlas.history 
zartrabedwars.atlas.leaderboard 
zartrabedwars.atlas.rewards 
zartrabedwars.atlas.report 
zartrabedwars.atlas.bypass.requirements 
zartrabedwars.atlas.vip 
zartrabedwars.atlas.staff 
zartrabedwars.atlas.admin 
zartrabedwars.atlas.manage.cases 
zartrabedwars.atlas.manage.reviewers 
zartrabedwars.atlas.view.identity 
zartrabedwars.atlas.view.staff-notes 
zartrabedwars.atlas.override.verdict 
zartrabedwars.atlas.suspend.reviewer 
zartrabedwars.atlas.unsuspend.reviewer 
zartrabedwars.atlas.invalidate.review 
zartrabedwars.atlas.reward 
zartrabedwars.atlas.debug 
zartrabedwars.atlas.performance ------------------------------------------------------------ 
ATLAS STAFF REVIEW ------------------------------------------------------------ 
Community verdicts must be sent to a staff-controlled final review system. 
Staff must be able to view: 
Replay 
Real Suspect Identity 
Community Verdict Distribution 
Reviewer Reputation 
Reviewer Accuracy 
Review Reasons 
Anti-Cheat Alerts 
Reports 
Evidence Markers 
Telemetry 
Previous Cases 
Previous Punishments where permitted 
Staff Notes 
Staff may: 
Confirm Verdict 
Reject Verdict 
Override Verdict 
Request Additional Reviews 
Close Case 
Reopen Case 
Escalate Case 
Apply Punishment through integration 
Mark False Report 
Mark Invalid Replay 
Protect Replay 
Archive Case ------------------------------------------------------------ 
ATLAS PUNISHMENT SAFETY ------------------------------------------------------------ 
Community verdicts must not directly apply permanent punishments by default. 
Any automated action must require an explicitly configured policy. 
Support configurable policies such as: 
Staff Review Required 
Trusted Reviewer Consensus 
Anti-Cheat Confirmation Required 
Minimum Reviewer Count 
Minimum Trusted Reviewer Count 
Minimum Confidence 
Minimum Accuracy Weight 
No automated permanent punishment may be enabled silently. ------------------------------------------------------------ 
ATLAS API ------------------------------------------------------------ 
Provide APIs for: 
Create Case 
Create Case from Replay 
Create Case from Report 
Create Case from Grim Alert 
Create Case from Vulcan Alert 
Reserve Case 
Release Case 
Assign Case 
Close Case 
Reopen Case 
Delete Case 
Submit Verdict 
Invalidate Review 
Override Verdict 
Query Case 
Query Reviewer 
Query Reviewer Accuracy 
Query Reviewer Reputation 
Query Verdict Distribution 
Register Verdict Type 
Register Reason Type 
Register Case Source 
Register Reward Provider 
Register Eligibility Provider 
Listen to Atlas Events 
Integrate External Moderation Systems ------------------------------------------------------------ 
ATLAS PLACEHOLDERS ------------------------------------------------------------ 
Include: 
atlas_eligible 
atlas_required_level 
atlas_required_games 
atlas_cases_reviewed 
atlas_accurate_reviews 
atlas_inaccurate_reviews 
atlas_accuracy 
atlas_recent_accuracy 
atlas_reputation 
atlas_tier 
atlas_review_streak 
atlas_best_review_streak 
atlas_rewards_earned 
atlas_suspended 
atlas_suspension_time 
atlas_open_cases 
atlas_queue_size 
atlas_reserved_case 
atlas_daily_reviews 
atlas_weekly_reviews ------------------------------------------------------------ 
ATLAS PERFORMANCE ------------------------------------------------------------ 
Atlas must not significantly affect gameplay servers. 
Use: 
Replay Service Separation where configured 
Async Case Loading 
Cached Reviewer Profiles 
Cached Queue Metadata 
Bounded Case Reservations 
Async Reward Processing 
Async Database Persistence 
Rate-Limited GUI Updates 
Cached Leaderboards 
Redis Synchronization 
Case Deduplication 
Adaptive Replay Loading ------------------------------------------------------------ 
ANTI-CHEAT INTEGRATION ------------------------------------------------------------ 
Integrate with: 
Grim Anti-Cheat 
Vulcan Anti-Cheat 
Support: 
Alert Events 
Check Name 
Check Category 
Violation Level 
Severity 
Timestamp 
Player UUID 
Player Name Snapshot 
Arena ID 
Map ID 
Mode ID 
Server ID 
Backend ID 
Replay ID 
Location 
Ping 
TPS 
MSPT 
Additional Metadata 
Replay Marker 
Evidence Marker 
Atlas Case Creation 
Staff Case Creation 
Staff Notification 
Discord Notification Hook 
Punishment Workflow Hook ------------------------------------------------------------ 
GRIM AND VULCAN PERFORMANCE ------------------------------------------------------------ 
Requirements: 
Use Event Hooks 
Avoid Polling 
Avoid Duplicate Alert Processing 
Avoid Duplicate Internal Checks 
Rate-Limit Repeated Alerts 
Group Related Alerts 
Use Bounded Queues 
Process Persistence Asynchronously 
Cache Provider Metadata 
Avoid Main-Thread Blocking 
Allow Per-Check Configuration 
Allow Per-Provider Thresholds 
Allow Both Providers Simultaneously 
Prevent Duplicate Cases 
Prevent Duplicate Replay Markers ------------------------------------------------------------ 
ANTI-CHEAT ALERT GUI ------------------------------------------------------------ 
Provide: 
Live Alerts 
Recent Alerts 
Player Alert History 
Check Filter 
Severity Filter 
Provider Filter 
Server Filter 
Arena Filter 
Replay Link 
Atlas Case Link 
Staff Case Link 
Player Spectate 
Player Freeze 
Acknowledge Alert 
Mute Alert 
Dismiss Alert 
Create Case 
Assign Case 
Export Evidence ------------------------------------------------------------ 
STATISTICS SYSTEM ------------------------------------------------------------ 
Create one authoritative statistics platform. 
All statistics must derive from the shared game event system. 
Avoid separate counters managed independently by unrelated modules. ------------------------------------------------------------ 
LIFETIME STATISTICS ------------------------------------------------------------ 
Track at minimum: 
Games Played 
Games Started 
Games Completed 
Wins 
Losses 
Draws 
Kills 
Deaths 
Assists 
Final Kills 
Final Deaths 
Beds Broken 
Beds Lost 
Current Winstreak 
Best Winstreak 
KDR 
FKDR 
WLR 
Win Percentage 
Playtime 
Lobby Time 
Game Time 
Spectator Time 
Resources Collected 
Iron Collected 
Gold Collected 
Diamonds Collected 
Emeralds Collected 
Custom Resources Collected 
Items Purchased 
Upgrades Purchased 
Traps Purchased 
Blocks Placed 
Blocks Broken 
TNT Placed 
Fireballs Used 
Bridge Eggs Used 
Pop-up Towers Used 
Potions Used 
Projectiles Fired 
Projectile Hits 
Void Deaths 
Disconnects 
Reconnects 
Rejoins 
Games Abandoned 
Perfect Games 
Beds Survived 
Damage Dealt where reliable 
Damage Received where reliable 
Replay Views 
Reports Submitted 
Atlas Cases Reviewed 
Atlas Accurate Verdicts ------------------------------------------------------------ 
STATISTICS DIMENSIONS ------------------------------------------------------------ 
Track statistics by: 
Lifetime 
Daily 
Weekly 
Monthly 
Season 
Event 
Mode 
Map 
Map ID 
Arena 
Arena Group 
Team Size 
Server 
Backend 
Proxy 
Public Game 
Private Game 
Ranked Game 
Tournament 
Party Size 
Solo Queue 
Party Queue 
Private game statistics must remain separate from public statistics by default. ------------------------------------------------------------ 
RATIO STATISTICS ------------------------------------------------------------ 
Provide: 
KDR 
FKDR 
WLR 
Bed Break Ratio 
Bed Loss Ratio 
Kills per Game 
Final Kills per Game 
Beds per Game 
Average Game Duration 
Win Percentage 
Final Death Rate 
Disconnect Rate 
Rejoin Rate 
Support configurable: 
Decimal Precision 
Rounding Mode 
Display Format 
Minimum Denominator 
Zero-Division Fallback ------------------------------------------------------------ 
WINSTREAK SYSTEM ------------------------------------------------------------ 
Implement: 
Current Winstreak 
Best Winstreak 
Per-Mode Winstreak 
Per-Arena-Group Winstreak 
Per-Team-Size Winstreak 
Seasonal Winstreak 
Daily Winstreak 
Party Winstreak where configured 
Ranked Winstreak 
Winstreak History 
Streak Start 
Streak End 
Streak Break Reason 
Streak Rewards 
Streak Milestones 
Streak Protection Option 
Staff Adjustment 
Migration 
Rollback 
Private games must not affect public winstreaks by default. 
------------------------------------------------------------ 
STATISTICS CONSISTENCY ------------------------------------------------------------ 
Statistics updates must be: 
Atomic 
Idempotent 
Event-Based 
Duplicate-Protected 
Recoverable 
Cross-Server Safe 
Transaction-Aware 
Auditable for Administrative Changes 
Prevent: 
Double Wins 
Double Final Kills 
Repeated Match-End Processing 
Cross-Server Duplicate Writes 
Replay Reprocessing Affecting Live Statistics 
Private Match Pollution 
Staff Test Match Pollution where configured ------------------------------------------------------------ 
STATISTICS STORAGE ------------------------------------------------------------ 
Support: 
SQLite 
MySQL 
MariaDB 
HikariCP 
Async Operations 
Caching 
Write-Behind 
Batching 
Database Indexes 
Schema Versioning 
Migration 
Backup 
Restore 
Import 
Export 
Repair 
Consistency Checks 
Redis Invalidation 
Player Data Deletion 
Player Data Anonymization where required ------------------------------------------------------------ 
STATISTICS ADMINISTRATION ------------------------------------------------------------ 
Support: 
View Player Statistics 
Set Statistic 
Add Statistic 
Remove Statistic 
Reset Statistic 
Reset Category 
Reset Season 
Recalculate Ratios 
Repair Player Data 
Merge Player Data 
Export Player Data 
Import Player Data 
Migrate Data 
Compare Cache and Database 
Audit Administrative Changes 
Dry Run 
Rollback ------------------------------------------------------------ 
STATISTICS GUI ------------------------------------------------------------ 
Provide: 
Player Statistics 
Lifetime Statistics 
Mode Statistics 
Map Statistics 
Arena Group Statistics 
Season Statistics 
Daily Statistics 
Weekly Statistics 
Monthly Statistics 
Ratios 
Winstreaks 
Match History 
Recent Performance 
Comparison View 
Admin Inspector 
Data Repair GUI 
Search 
Filters 
Sorting 
Pagination ------------------------------------------------------------ 
STATISTICS COMMANDS ------------------------------------------------------------ 
Provide: 
/stats 
/stats <player> 
/stats mode <mode> 
/stats map <map> 
/stats group <group> 
/stats season 
/stats daily 
/stats weekly 
/stats monthly 
/stats compare <player> 
/stats admin 
/stats admin set 
/stats admin add 
/stats admin remove 
/stats admin reset 
/stats admin repair 
/stats admin migrate 
/stats admin import 
/stats admin export ------------------------------------------------------------ 
STATISTICS API ------------------------------------------------------------ 
Provide APIs for: 
Read Statistic 
Read Multiple Statistics 
Update Statistic through Authorized Service 
Register Custom Statistic 
Register Statistic Dimension 
Register Ratio 
Query Match History 
Query Winstreak 
Reset Statistic 
Listen to Statistic Events 
Export Data 
Register Leaderboard Source ------------------------------------------------------------ 
LEADERBOARD SYSTEM ------------------------------------------------------------ 
Create a unified leaderboard engine. 
Support: 
Wins 
Kills 
Final Kills 
Beds Broken 
Winstreak 
Best Winstreak 
Level 
Experience 
Coins 
KDR 
FKDR 
WLR 
Playtime 
Quests 
Achievements 
Battle Pass 
Atlas Reviews 
Atlas Accuracy 
Custom Statistics ------------------------------------------------------------ 
LEADERBOARD DIMENSIONS ------------------------------------------------------------ 
Support: 
Lifetime 
Daily 
Weekly 
Monthly 
Seasonal 
Mode 
Map 
Arena Group 
Team Size 
Server 
Global Network 
Friends 
Guild where integrated 
Party where appropriate ------------------------------------------------------------ 
LEADERBOARD PRESENTATION ------------------------------------------------------------ 
Support: 
GUI Leaderboards 
Holographic Leaderboards 
NPC Leaderboards 
Chat Leaderboards 
Command Leaderboards 
Scoreboard Leaderboards 
Discord API 
External API 
PlaceholderAPI 
DecentHolograms 
Internal Holograms 
Citizens 
ZNPCs Plus 
Packet NPC ------------------------------------------------------------ 
LEADERBOARD FEATURES ------------------------------------------------------------ 
Support: 
Pagination 
Search 
Player Position 
Nearby Positions 
Top N 
Filters 
Sorting 
Season Selector 
Mode Selector 
Arena Group Selector 
Offline Players 
Hidden Players 
Privacy Settings 
Staff Exclusion 
Banned Player Exclusion 
Test Account Exclusion 
Cached Rankings 
Scheduled Refresh 
Incremental Refresh 
Manual Refresh ------------------------------------------------------------ 
LEADERBOARD PERFORMANCE ------------------------------------------------------------ 
Avoid full-table ranking queries on every request. 
Use: 
Precomputed Rankings 
Materialized Cache 
Scheduled Updates 
Indexed Queries 
Top-N Cache 
Per-Player Rank Cache 
Redis Synchronization 
Async Refresh 
Stale-While-Revalidate 
Configurable Refresh Intervals 
------------------------------------------------------------ 
PLACEHOLDERAPI SYSTEM ------------------------------------------------------------ 
PlaceholderAPI support is mandatory. 
Create a native integrated expansion. 
Every meaningful player, arena, team, map, mode, progression, replay, Atlas, statistics and 
network value must be exposed where appropriate. ------------------------------------------------------------ 
PLACEHOLDER NAMING ------------------------------------------------------------ 
Use one stable namespace. 
Example: 
%zartrabedwars_<identifier>% 
Placeholder names must be: 
Consistent 
Documented 
Stable 
Backward-Compatible 
Validated ------------------------------------------------------------ 
PLAYER STATISTIC PLACEHOLDERS ------------------------------------------------------------ 
Include: 
games_played 
wins 
losses 
draws 
kills 
deaths 
assists 
final_kills 
final_deaths 
beds_broken 
beds_lost 
winstreak 
best_winstreak 
kdr 
fkdr 
wlr 
win_percentage 
playtime 
resources_collected 
iron_collected 
gold_collected 
diamonds_collected 
emeralds_collected 
items_purchased 
upgrades_purchased 
blocks_placed 
blocks_broken 
void_deaths 
disconnects 
rejoins ------------------------------------------------------------ 
DIMENSIONAL PLACEHOLDERS ------------------------------------------------------------ 
Support dynamic placeholders for: 
Mode 
Map 
Map ID 
Arena Group 
Team Size 
Season 
Daily 
Weekly 
Monthly 
Lifetime ------------------------------------------------------------ 
PROGRESSION PLACEHOLDERS ------------------------------------------------------------ 
Include: 
level 
experience 
experience_required 
experience_progress 
experience_percentage 
prestige 
prestige_name 
prestige_format 
coins 
currency_<id> 
battle_pass_tier 
battle_pass_experience 
battle_pass_premium 
achievement_points 
completed_achievements 
quest_progress 
completed_quests 
owned_cosmetics 
equipped_cosmetic_<category> 
titles 
badges ------------------------------------------------------------ 
ARENA PLACEHOLDERS ------------------------------------------------------------ 
Include: 
arena_id 
arena_display_name 
arena_state 
arena_players 
arena_max_players 
arena_min_players 
arena_countdown 
arena_elapsed_time 
arena_remaining_time 
arena_mode 
arena_group 
map_id 
map_display_name 
map_author 
team_count 
alive_teams 
spectator_count 
waiting_count ------------------------------------------------------------ 
TEAM PLACEHOLDERS ------------------------------------------------------------ 
Include: 
team_id 
team_display_name 
team_color 
team_players 
team_alive_players 
team_bed_alive 
team_kills 
team_final_kills 
team_beds_broken 
team_upgrades 
team_traps 
team_generator_level ------------------------------------------------------------ 
SERVER AND NETWORK PLACEHOLDERS ------------------------------------------------------------ 
Include: 
server_id 
backend_id 
proxy_id 
deployment_mode 
online_players 
players_in_games 
players_in_lobby 
active_arenas 
waiting_arenas 
running_arenas 
resetting_arenas 
loaded_worlds 
managed_worlds 
queue_size 
redis_status 
database_status 
proxy_status 
cloudnet_status 
------------------------------------------------------------ 
REPLAY AND ATLAS PLACEHOLDERS ------------------------------------------------------------ 
Include: 
replay_count 
recent_replay_id 
atlas_eligible 
atlas_required_level 
atlas_required_games 
atlas_cases_reviewed 
atlas_accurate_reviews 
atlas_accuracy 
atlas_reputation 
atlas_tier 
atlas_suspended 
atlas_open_cases 
atlas_case_queue ------------------------------------------------------------ 
LEADERBOARD PLACEHOLDERS ------------------------------------------------------------ 
Support: 
Top Player Name 
Top Player Value 
Nth Player Name 
Nth Player Value 
Current Player Rank 
Current Player Value 
Nearby Rank 
Mode Leaderboard 
Season Leaderboard 
Arena Group Leaderboard ------------------------------------------------------------ 
PLACEHOLDER CONTEXT ------------------------------------------------------------ 
Support: 
Player Context 
Offline Player Context 
Arena Context 
Team Context 
Server Context 
No-Player Context where appropriate 
Invalid context must return a safe configurable fallback. ------------------------------------------------------------ 
PLACEHOLDER PERFORMANCE ------------------------------------------------------------ 
Placeholder evaluation must never execute synchronous database queries. 
Use: 
Cached Values 
Precomputed Ratios 
Async Refresh 
Bounded Expiration 
Batch Loading 
Per-Tick Deduplication 
Slow Placeholder Detection 
Placeholder Metrics ------------------------------------------------------------ 
PLACEHOLDER FORMATTING ------------------------------------------------------------ 
Support: 
Raw Values 
Formatted Values 
Compact Numbers 
Duration Formatting 
Percentage Formatting 
Decimal Precision 
Locale-Aware Formatting 
Color Formatting 
Fallback Values 
Null Values 
Unknown Values ------------------------------------------------------------ 
PLACEHOLDER ADMINISTRATION ------------------------------------------------------------ 
Provide: 
Placeholder List 
Placeholder Search 
Placeholder Test 
Placeholder Debug 
Placeholder Performance Report 
Placeholder Cache Status 
Placeholder Reload 
Placeholder Documentation Generator 
Commands: 
/zbw placeholder list 
/zbw placeholder search <query> 
/zbw placeholder test <player> <placeholder> 
/zbw placeholder debug <placeholder> 
/zbw placeholder performance ------------------------------------------------------------ 
PLACEHOLDER API FOR DEVELOPERS ------------------------------------------------------------ 
Provide APIs to: 
Register Placeholder 
Unregister Placeholder 
Register Dynamic Placeholder Family 
Register Formatter 
Register Context Resolver 
Query Placeholder Metadata 
Test Placeholder 
Listen to Placeholder Errors 
Generate Placeholder Documentation ------------------------------------------------------------ 
DISCORD STATISTICS INTEGRATION ------------------------------------------------------------ 
Provide a secure integration layer for: 
Player Statistics 
Leaderboards 
Match Summaries 
Winstreaks 
Levels 
Prestiges 
Quest Progress where permitted 
Achievement Progress where permitted 
Atlas Reviewer Statistics where permitted 
Server Status 
Arena Status 
Do not hardcode one Discord bot implementation. 
Use adapter APIs. ------------------------------------------------------------ 
EXTERNAL STATISTICS API ------------------------------------------------------------ 
Provide an optional secure external statistics API. 
Support: 
Authentication 
Rate Limiting 
Permission Scopes 
Player Privacy 
Cached Responses 
Pagination 
Versioned Endpoints 
Audit Logging 
Disable Option 
Never expose sensitive staff, report, anticheat or Atlas identity data without explicit 
authorization. ------------------------------------------------------------ 
DATA PRIVACY ------------------------------------------------------------ 
Support player settings for: 
Leaderboard Visibility 
Replay History Visibility 
Profile Visibility 
Atlas Review History Visibility 
Recent Match Visibility 
External API Visibility 
Player privacy must never block authorized staff access to moderation evidence. ------------------------------------------------------------ 
MIGRATION ------------------------------------------------------------ 
Provide migration support where legally and technically appropriate for: 
BedWars Statistics 
BedWars1058-Compatible Statistics 
Winstreak Data 
KDR 
FKDR 
WLR 
Arena Group Statistics 
Replay Metadata 
Leaderboard Data 
Placeholder Aliases 
Migration must support: 
Dry Run 
Backup 
Validation 
Mapping 
Duplicate Detection 
Progress 
Error Report 
Rollback 
Audit Log ------------------------------------------------------------ 
TESTING REQUIREMENTS ------------------------------------------------------------ 
Replay tests must cover: 
Recording Lifecycle 
Playback Timing 
Event Ordering 
Corrupt Data 
Interrupted Recording 
Retention 
Permissions 
Privacy 
Provider Failure 
Performance 
Atlas tests must cover: 
Eligibility 
VIP Bypass 
Staff Bypass 
Anonymization 
Conflict Prevention 
Verdict Submission 
Reviewer Accuracy 
Anti-Abuse 
Rewards 
Staff Override 
Statistics tests must cover: 
Duplicate Event Prevention 
Ratio Calculation 
Cross-Server Updates 
Private and Public Separation 
Winstreak Reset 
Database Failure 
Migration 
Leaderboard tests must cover: 
Ranking Accuracy 
Ties 
Caching 
Refresh 
Privacy 
Season Reset 
Placeholder tests must cover: 
Registered Placeholders 
Invalid Context 
Offline Player 
Cache Behavior 
Performance 
Formatting 
Dynamic Placeholder Parsing ------------------------------------------------------------ 
GENERAL COMPLETION RULE ------------------------------------------------------------ 
Every system in Part 4D is mandatory. 
Replay is complete only when recording, playback, storage, retention, recovery, privacy, 
GUI, commands, permissions, API, documentation and testing are complete. 
Atlas is complete only when eligibility, anonymization, case creation, review, verdicts, 
reputation, anti-abuse, rewards, staff review, GUI, commands, permissions, API, 
documentation and testing are complete. 
Statistics are complete only when all required statistics, dimensions, ratios, winstreaks, 
storage, consistency, administration, GUI, API, migration and testing are complete. 
PlaceholderAPI is complete only when every meaningful value is exposed where 
appropriate, no synchronous database queries occur during normal evaluation, 
documentation is generated, dynamic placeholders are validated and performance tests 
pass. ------------------------------------------------------------ 
MANDATORY FUNCTIONAL SCOPE 
PART 4E 
------------------------------------------------------------ 
Everything described in this section is mandatory unless a requirement is explicitly marked 
MAY. 
No integration, command, permission, GUI, API, configuration section, test or documentation 
deliverable may be omitted because implementation is difficult. 
The platform must operate as one coherent product in both single-server and network 
deployments. ------------------------------------------------------------ 
SUPPORTED DEPLOYMENT MODES ------------------------------------------------------------ 
The platform MUST support two complete deployment modes. 
MODE 1: 
SHARED_SERVER 
Multiple BedWars arenas operate on one Paper server. 
Each arena may use: 
A dedicated world 
A cloned template world 
A SlimeWorldManager world 
A Multiverse-Core managed world 
A native Bukkit world 
The server may contain forty or more managed worlds. 
The platform must optimize loading, unloading, chunk retention, entity processing and world 
resets. 
MODE 2: 
SCALABLE_PROXY 
BedWars operates across multiple backend servers connected through: 
Velocity 
BungeeCord 
CloudNet 
Redis 
The platform must support: 
One arena per backend 
Multiple arenas per backend 
One arena group per backend 
Multiple groups per backend 
Dynamic backend registration 
Static backend registration 
Server health detection 
Arena state synchronization 
Cross-server queues 
Cross-server parties 
Cross-server private games 
Cross-server rejoin 
Cross-server play again 
Cross-server statistics 
Cross-server punishments 
Cross-server Atlas cases 
Cross-server replay availability 
Cross-server maintenance mode 
Cross-server announcements ------------------------------------------------------------ 
DEPLOYMENT CONFIGURATION 
------------------------------------------------------------ 
The deployment mode must be configurable. 
Example: 
deployment: 
mode: SHARED_SERVER 
or: 
deployment: 
mode: SCALABLE_PROXY 
proxy-provider: VELOCITY 
service-discovery: CLOUDNET 
synchronization-provider: REDIS 
Changing deployment mode must not require rewriting arena definitions. 
The configuration system must validate that all required services for the selected deployment 
mode are available. ------------------------------------------------------------ 
VELOCITY SUPPORT ------------------------------------------------------------ 
Provide a dedicated Velocity module. 
Support: 
Backend registration 
Server status 
Arena status 
Player transfer 
Party transfer 
Private game transfer 
Queue management 
Rejoin routing 
Play Again routing 
Maintenance state 
Proxy commands 
Proxy permissions 
Proxy messaging 
Signed plugin messages 
Secure communication 
Server heartbeat 
Failover routing 
Graceful backend removal 
Duplicate player protection 
Connection retry 
Fallback lobby 
Proxy API 
Proxy events 
Proxy configuration 
Proxy diagnostics ------------------------------------------------------------ 
BUNGEECORD SUPPORT ------------------------------------------------------------ 
Provide equivalent BungeeCord support. 
BungeeCord and Velocity must share an internal proxy abstraction. 
Do not duplicate business logic between proxy implementations. 
The platform must expose the same BedWars behavior regardless of the selected proxy. ------------------------------------------------------------ 
CLOUDNET SUPPORT ------------------------------------------------------------ 
Provide complete CloudNet integration. 
Support: 
Dynamic service creation 
Dynamic service shutdown 
Arena template deployment 
Arena group templates 
Service metadata 
Service health 
Player count 
Arena capacity 
Automatic scaling 
Minimum service count 
Maximum service count 
Warm pool 
Idle shutdown 
Crash replacement 
Service allocation 
Queue-driven scaling 
Private game service creation 
Replay service creation where configured 
CloudNet events 
CloudNet commands 
CloudNet permissions 
CloudNet GUI status 
CloudNet diagnostics ------------------------------------------------------------ 
REDIS SUPPORT ------------------------------------------------------------ 
Redis integration is mandatory for scalable synchronization. 
Support: 
Player state 
Arena state 
Queue state 
Party state 
Private game state 
Server state 
Winstreak state 
Statistics cache invalidation 
Placeholder cache invalidation 
Punishment synchronization 
Atlas case synchronization 
Replay metadata synchronization 
Announcements 
Distributed locks 
Leader election where required 
Pub/Sub 
Streams where appropriate 
Key expiration 
Namespace configuration 
Connection pooling 
Reconnect strategy 
Circuit breaker 
Backpressure 
Message deduplication 
Message versioning 
Secure authentication 
TLS where supported 
Redis health checks 
Redis metrics 
Redis diagnostics 
Redis data cleanup 
The platform must never use unbounded Redis polling. 
Prefer event-based synchronization. ------------------------------------------------------------ 
DATABASE INTEGRATIONS ------------------------------------------------------------ 
Support: 
SQLite 
MySQL 
MariaDB 
HikariCP 
The database layer must support: 
Schema versioning 
Automatic migrations 
Migration validation 
Migration rollback where possible 
Connection pool metrics 
Read/write separation where configured 
Transaction handling 
Retry policy 
Deadlock handling 
Query timeouts 
Prepared statements 
Batch operations 
Indexes 
Data integrity constraints 
Backup 
Restore 
Export 
Import 
Data repair 
Player-data deletion 
Conflict resolution 
Cross-server consistency 
Database health GUI 
Database commands 
Database permissions 
Database API 
------------------------------------------------------------ 
PLACEHOLDERAPI ------------------------------------------------------------ 
PlaceholderAPI is mandatory. 
The plugin must include a native expansion. 
Every meaningful value must have a placeholder when technically appropriate. 
Categories must include: 
Player identity 
Player profile 
Level 
Experience 
Prestige 
Coins 
Custom currencies 
Wins 
Losses 
Games played 
Kills 
Deaths 
Final kills 
Final deaths 
Beds broken 
Beds lost 
Assists 
KDR 
FKDR 
WLR 
Current winstreak 
Best winstreak 
Per-mode statistics 
Per-map statistics 
Per-arena statistics 
Per-group statistics 
Per-season statistics 
Daily statistics 
Weekly statistics 
Monthly statistics 
Lifetime statistics 
Quest progress 
Quest completion 
Tracked quest 
Achievement progress 
Achievement points 
Battle Pass tier 
Battle Pass progress 
Premium state 
Cosmetics owned 
Cosmetics equipped 
Titles 
Badges 
Party information 
Team information 
Arena information 
Map information 
Map display name 
Map internal ID 
Mode information 
Generator state 
Upgrade state 
Shop state 
Queue state 
Proxy server 
Backend server 
CloudNet service 
Redis state 
Database state 
Replay state 
Atlas reviewer state 
Atlas accuracy 
Atlas cases reviewed 
Anticheat alerts 
Vulcan violations 
Grim violations 
Private game information 
Server health 
TPS 
Memory 
Player count 
Arena count 
World count 
Placeholder output must be cached appropriately. 
Placeholder processing must never perform synchronous database queries. 
Every placeholder must be documented with: 
Identifier 
Description 
Return type 
Example 
Scope 
Null or unavailable behavior 
Performance notes 
Required integration ------------------------------------------------------------ 
VAULT SUPPORT ------------------------------------------------------------ 
Provide Vault integration for: 
Economy 
Permissions where required 
Chat metadata where required 
The plugin must not assume Vault is the only economy or permissions provider. 
Use internal abstractions. ------------------------------------------------------------ 
LUCKPERMS SUPPORT ------------------------------------------------------------ 
Provide optimized LuckPerms compatibility. 
Support: 
Prefix 
Suffix 
Meta 
Contexts 
Temporary permissions 
Rank-based cosmetic access 
Rank-based Atlas bypass 
Rank-based reward multipliers 
Rank-based private game access 
Rank-based queue priority where configured 
Permission synchronization 
Context-aware permissions 
LuckPerms data must be cached responsibly. ------------------------------------------------------------ 
PROTOCOLLIB AND PACKET ABSTRACTION ------------------------------------------------------------ 
Support ProtocolLib. 
Provide an internal packet abstraction. 
Use packet operations only when they provide a real benefit. 
Packet-based features may include: 
Internal NPCs 
Cosmetics 
Replay visualization 
Spectator tools 
Holograms 
Fake entities 
Tab updates 
Scoreboard updates 
Visual effects 
Packet code must be isolated from gameplay logic. 
Packet rate limits and compatibility safeguards are mandatory. ------------------------------------------------------------ 
WORLD INTEGRATIONS ------------------------------------------------------------ 
Provide complete compatibility with: 
WorldEdit 
FastAsyncWorldEdit 
WorldGuard 
SlimeWorldManager 
Multiverse-Core 
The platform must use a provider abstraction. 
Operators must be able to select the preferred world provider. 
Support: 
Map import 
Map export 
Map duplication 
Map backup 
Map restore 
Map snapshots 
Region selection 
Arena bounds 
Protected regions 
Build regions 
Void level 
Spawn validation 
Template conversion 
Fast reset 
Async file operations 
Provider diagnostics 
Provider migration 
World provider fallback where safe ------------------------------------------------------------ 
NPC INTEGRATIONS ------------------------------------------------------------ 
Support: 
Internal packet-based NPCs 
Citizens 
ZNPCs Plus 
NPCs must support: 
Arena selector 
Map selector 
Mode selector 
Quick join 
Shopkeeper 
Upgrade shopkeeper 
Quest menu 
Cosmetics menu 
Statistics menu 
Profile menu 
Private games 
Atlas menu 
Replay menu 
Admin menu 
NPC skin 
NPC name 
NPC hologram 
NPC animation 
NPC rotation 
NPC interaction cooldown 
NPC permissions 
NPC visibility 
Per-world NPCs 
Per-arena NPCs 
Per-mode NPCs 
Provider switching 
NPC import 
NPC export 
NPC diagnostics ------------------------------------------------------------ 
HOLOGRAM INTEGRATIONS ------------------------------------------------------------ 
Support: 
Internal holograms 
DecentHolograms 
Holograms must support: 
Leaderboards 
Generator countdowns 
Arena status 
Map name 
NPC labels 
Quest progress 
Statistics 
Replay markers 
Atlas information 
Setup instructions 
Admin diagnostics 
Hologram updates must be rate-limited and cached. ------------------------------------------------------------ 
PARTY INTEGRATIONS 
------------------------------------------------------------ 
Provide: 
Native party system 
AlessioDP Parties integration 
Compatibility layer for other established party plugins where practical 
Party functionality must include: 
Create 
Invite 
Accept 
Decline 
Leave 
Disband 
Kick 
Promote 
Transfer leadership 
Party chat 
Party settings 
Party privacy 
Party queue 
Party team assignment 
Party private games 
Party map selection 
Party mode selection 
Party cross-server transfer 
Party rejoin 
Party play again 
Party quest progress 
Party challenge progress 
Party API 
Party events 
Party placeholders 
Party commands 
Party permissions 
Party GUI 
Provider migration ------------------------------------------------------------ 
ANTICHEAT INTEGRATIONS ------------------------------------------------------------ 
Provide optimized integrations with: 
Grim Anti-Cheat 
Vulcan Anti-Cheat 
Support: 
Alert ingestion 
Violation level 
Check name 
Check category 
Check severity 
Player ping 
Server TPS 
Timestamp 
Arena 
Map 
Mode 
Replay association 
Atlas case creation 
Staff notification 
Staff GUI 
Punishment workflow 
Evidence retention 
Cross-server synchronization 
Duplicate alert suppression 
Rate limiting 
Alert grouping 
Alert history 
API events 
PlaceholderAPI values 
Discord notification hooks 
The plugin must not duplicate expensive checks already performed by Grim or Vulcan 
without a distinct purpose. 
No polling-heavy integration is allowed. 
Use APIs and events when available. ------------------------------------------------------------ 
VIA AND CROSS-VERSION SUPPORT ------------------------------------------------------------ 
Support compatibility with: 
ViaVersion 
ViaBackwards 
ViaRewind 
The platform must validate client-version-sensitive functionality. 
Features such as materials, particles, sounds, packets and GUI items must use compatibility 
abstractions. ------------------------------------------------------------ 
GEYSER AND FLOODGATE SUPPORT ------------------------------------------------------------ 
Support: 
Geyser 
Floodgate 
Bedrock players must receive usable alternatives for interactions that depend on 
Java-specific input. 
Provide Bedrock-safe: 
GUIs 
Shop navigation 
Chat input 
Commands 
NPC interactions 
Replay controls where feasible 
Atlas controls where feasible 
Team selection 
Quick communications 
Custom item behavior 
The platform must document limitations honestly. 
------------------------------------------------------------ 
GLOBAL GUI FRAMEWORK ------------------------------------------------------------ 
Create one unified GUI framework. 
All GUIs must use consistent: 
Navigation 
Back buttons 
Close behavior 
Pagination 
Search 
Filtering 
Sorting 
Confirmation 
Error display 
Loading display 
Permission validation 
Localization 
Sound 
Animation 
Item rendering 
Placeholder processing 
Async data loading 
GUI state retention 
Every GUI must have a stable internal ID. ------------------------------------------------------------ 
MANDATORY PLAYER GUIS ------------------------------------------------------------ 
Provide at minimum: 
Main BedWars Menu 
Arena Selector 
Map Selector 
Mode Selector 
Group Selector 
Quick Join Menu 
Team Selector 
Rejoin Menu 
Play Again Menu 
Profile Menu 
Statistics Menu 
Leaderboard Menu 
Quest Menu 
Achievement Menu 
Challenge Menu 
Battle Pass Menu 
Cosmetics Menu 
Shop Menu 
Quick Buy Editor 
Private Game Menu 
Party Menu 
Spectator Menu 
Replay Menu 
Atlas Menu 
Player Settings Menu 
Language Menu ------------------------------------------------------------ 
MANDATORY ADMIN GUIS ------------------------------------------------------------ 
Provide at minimum: 
Admin Dashboard 
Arena Manager 
Map Manager 
Duplicate Map GUI 
World Manager 
Setup Wizard 
Setup Validator 
Team Editor 
Generator Editor 
Shop Editor 
Upgrade Editor 
NPC Manager 
Hologram Manager 
Quest Editor 
Achievement Editor 
Cosmetic Editor 
Battle Pass Editor 
Reward Editor 
Statistics Manager 
Player Data Inspector 
Database Manager 
Redis Manager 
Proxy Manager 
CloudNet Manager 
Integration Manager 
Replay Manager 
Atlas Case Manager 
Anticheat Alert Manager 
Permission Inspector 
Command Inspector 
Placeholder Browser 
Configuration Editor 
Migration Manager 
Backup Manager 
Restore Manager 
Health Dashboard 
Performance Dashboard 
Debug Dashboard 
Audit Log Viewer ------------------------------------------------------------ 
GUI EDITOR ------------------------------------------------------------ 
Administrators must be able to edit GUI layouts in-game. 
Support: 
Slot placement 
Icon selection 
Name 
Lore 
Action 
Permission 
Condition 
Sound 
Animation 
Custom Model Data 
Placeholder content 
Back button 
Close button 
Pagination control 
Search control 
Preview 
Validation 
Duplicate GUI 
Import 
Export 
Reset to default 
Undo where practical 
------------------------------------------------------------ 
COMMAND FRAMEWORK ------------------------------------------------------------ 
Use one unified command framework. 
Every command must support: 
Permission validation 
Argument validation 
Tab completion 
Localization 
Console compatibility where appropriate 
Player-only validation where appropriate 
Clickable help 
Usage examples 
Alias support 
Cooldowns where required 
Audit logging for sensitive actions 
Structured errors 
Command API 
Command documentation ------------------------------------------------------------ 
PLAYER COMMANDS ------------------------------------------------------------ 
Provide commands for: 
BedWars main menu 
Join 
Quick Join 
Random Join 
Map selection 
Mode selection 
Arena selection 
Leave 
Rejoin 
Play Again 
Team selection 
Spectate 
Statistics 
Leaderboards 
Profile 
Settings 
Language 
Party 
Private Games 
Quest 
Achievements 
Challenges 
Battle Pass 
Cosmetics 
Replay 
Atlas 
Report 
Shop configuration 
Quick Buy 
Help ------------------------------------------------------------ 
STAFF COMMANDS ------------------------------------------------------------ 
Provide commands for: 
Staff mode 
Vanish 
Spectate 
Follow player 
Freeze player 
Unfreeze player 
Inspect inventory 
Inspect ender chest 
Player information 
Ping 
CPS 
Reach data 
Movement data 
Anticheat alerts 
Replay evidence 
Atlas cases 
Reports 
Warnings 
Mute integration hooks 
Kick 
Ban integration hooks 
Teleport 
Random spectate 
Staff chat 
Admin chat 
Staff notifications ------------------------------------------------------------ 
ADMIN COMMANDS ------------------------------------------------------------ 
Provide complete commands for: 
Arena create 
Arena edit 
Arena delete 
Arena duplicate 
Arena enable 
Arena disable 
Arena validate 
Arena clone 
Arena import 
Arena export 
Arena backup 
Arena restore 
Map create 
Map edit 
Map delete 
Map duplicate 
Map rename display name 
Map inspect internal ID 
Map import 
Map export 
Map backup 
Map restore 
World load 
World unload 
World clone 
World reset 
World migrate 
Setup wizard 
Setup save 
Setup cancel 
Setup validate 
Team management 
Generator management 
Shop management 
Upgrade management 
NPC management 
Hologram management 
Quest management 
Achievement management 
Cosmetic management 
Battle Pass management 
Player-data management 
Statistics management 
Replay management 
Atlas management 
Anticheat integration management 
Database management 
Redis management 
Proxy management 
CloudNet management 
Party management 
Configuration reload 
Configuration validate 
Migration 
Backup 
Restore 
Debug 
Diagnostics 
Health 
Performance 
Audit logs 
Plugin information 
Version information ------------------------------------------------------------ 
PERMISSION FRAMEWORK ------------------------------------------------------------ 
Permissions must be granular. 
Do not depend on hardcoded rank names. 
Create permission categories for: 
Player 
VIP 
Staff 
Helper 
Moderator 
Administrator 
Owner 
Console 
Developer 
Every action must have a dedicated permission where appropriate. ------------------------------------------------------------ 
PERMISSION ACTIONS ------------------------------------------------------------ 
Provide distinct permissions for: 
View 
Use 
Create 
Edit 
Delete 
Duplicate 
Import 
Export 
Enable 
Disable 
Start 
Stop 
Force 
Reload 
Reset 
Backup 
Restore 
Migrate 
Inspect 
Debug 
Bypass 
Manage 
Grant 
Revoke 
Set 
Add 
Remove 
Approve 
Reject 
Override 
View identities 
View hidden data 
View private data ------------------------------------------------------------ 
ATLAS PERMISSIONS ------------------------------------------------------------ 
Include at minimum: 
zartrabedwars.atlas.use 
zartrabedwars.atlas.review 
zartrabedwars.atlas.skip 
zartrabedwars.atlas.stats 
zartrabedwars.atlas.history 
zartrabedwars.atlas.leaderboard 
zartrabedwars.atlas.bypass.requirements 
zartrabedwars.atlas.vip 
zartrabedwars.atlas.staff 
zartrabedwars.atlas.admin 
zartrabedwars.atlas.manage-cases 
zartrabedwars.atlas.view-identities 
zartrabedwars.atlas.override-verdict 
zartrabedwars.atlas.suspend-reviewer 
zartrabedwars.atlas.reward 
Default Atlas requirements: 
Minimum BedWars Level: 20 
Minimum Games Played: 100 
Both requirements must be satisfied by normal players. 
Staff may always access. 
VIP players may bypass requirements only with the dedicated permission. 
All values must be configurable. ------------------------------------------------------------ 
PUBLIC DEVELOPER API ------------------------------------------------------------ 
Provide a stable documented public API. 
Modules must include: 
Arena API 
Map API 
World API 
Game API 
Player API 
Team API 
Party API 
Shop API 
Upgrade API 
Generator API 
Quest API 
Achievement API 
Challenge API 
Battle Pass API 
Cosmetic API 
Currency API 
Reward API 
Statistics API 
Leaderboard API 
Replay API 
Atlas API 
Anticheat Integration API 
Proxy API 
Database API 
Configuration API 
GUI API 
Command API 
Permission API 
NPC API 
Hologram API 
Placeholder API 
Integration API 
Health API 
Performance API ------------------------------------------------------------ 
EVENT API ------------------------------------------------------------ 
Provide cancellable and non-cancellable events where appropriate. 
Events must cover: 
Arena lifecycle 
Game lifecycle 
Player join 
Player leave 
Player rejoin 
Team assignment 
Bed destruction 
Kill 
Final kill 
Respawn 
Elimination 
Victory 
Draw 
Shop purchase 
Upgrade purchase 
Generator spawn 
Quest progress 
Quest completion 
Achievement completion 
Battle Pass progress 
Cosmetic equip 
Reward grant 
Currency transaction 
Statistics update 
Replay start 
Replay save 
Atlas case creation 
Atlas verdict 
Anticheat alert 
Proxy transfer 
Database migration 
Configuration reload 
Integration state change 
API events must document thread context. ------------------------------------------------------------ 
CONFIGURATION SYSTEM ------------------------------------------------------------ 
Every operator-facing system must be configurable. 
Configuration files must be logically separated. 
Possible files include: 
config.yml 
deployment.yml 
database.yml 
redis.yml 
proxy.yml 
cloudnet.yml 
arenas.yml 
maps.yml 
modes.yml 
shops.yml 
upgrades.yml 
generators.yml 
items.yml 
quests.yml 
achievements.yml 
challenges.yml 
battlepass.yml 
cosmetics.yml 
rewards.yml 
statistics.yml 
placeholders.yml 
replay.yml 
atlas.yml 
anticheat.yml 
parties.yml 
npcs.yml 
holograms.yml 
gui.yml 
messages.yml 
permissions.yml 
performance.yml 
security.yml 
integrations.yml 
------------------------------------------------------------ 
CONFIG COMMENTS ------------------------------------------------------------ 
Every configuration option must include comments describing: 
Purpose 
Accepted values 
Default value 
Example 
Dependencies 
Performance impact 
Security impact 
Reload support 
Restart requirement 
Compatibility limitations 
Deprecation state 
No important configuration option may be undocumented. ------------------------------------------------------------ 
CONFIGURATION VALIDATION ------------------------------------------------------------ 
Provide: 
Startup validation 
Manual validation command 
GUI validation 
Unknown key detection 
Missing key detection 
Type validation 
Range validation 
Dependency validation 
Permission validation 
Material validation 
Sound validation 
Particle validation 
World validation 
Arena validation 
Placeholder validation 
Migration validation 
Automatic backup before migration 
Human-readable error reports ------------------------------------------------------------ 
CONFIGURATION RELOAD ------------------------------------------------------------ 
Provide safe targeted reloads. 
Do not reload the entire plugin when only one subsystem changed. 
Support: 
Messages reload 
GUI reload 
Shop reload 
Quest reload 
Cosmetic reload 
Integration reload 
Placeholder reload 
Arena configuration reload where safe 
The system must clearly state when a full restart is required. ------------------------------------------------------------ 
LOCALIZATION ------------------------------------------------------------ 
Provide a complete localization framework. 
Support: 
Multiple languages 
Per-player language 
Server default language 
Fallback language 
MiniMessage 
Adventure Components 
RGB 
Gradients 
Pluralization 
Parameterized messages 
PlaceholderAPI 
Clickable messages 
Hover text 
Localized GUI names 
Localized lore 
Localized commands help 
Localized errors 
Live language switching 
Language import 
Language export 
Translation completeness reports ------------------------------------------------------------ 
DOCUMENTATION DELIVERABLES ------------------------------------------------------------ 
Codex must produce complete documentation. 
Documentation is part of the product. 
Deliver: 
Installation Guide 
Quick Start Guide 
Shared Server Guide 
Proxy Network Guide 
Velocity Guide 
BungeeCord Guide 
CloudNet Guide 
Redis Guide 
Database Guide 
Arena Setup Guide 
Map Management Guide 
Duplicate Map Guide 
World Provider Guide 
Shop Guide 
Upgrade Guide 
Generator Guide 
Quest Guide 
Achievement Guide 
Battle Pass Guide 
Cosmetics Guide 
Statistics Guide 
PlaceholderAPI Guide 
Replay Guide 
Atlas Guide 
Grim Integration Guide 
Vulcan Integration Guide 
Party Integration Guide 
NPC Integration Guide 
API Developer Guide 
Migration Guide 
Backup and Restore Guide 
Performance Tuning Guide 
Security Guide 
Troubleshooting Guide 
FAQ 
Changelog 
Upgrade Notes ------------------------------------------------------------ 
COMMAND DOCUMENTATION ------------------------------------------------------------ 
Produce a complete command reference. 
For every command document: 
Command 
Aliases 
Description 
Syntax 
Arguments 
Examples 
Permission 
Default access 
Console support 
Cooldown 
Related GUI 
Related configuration 
Failure cases ------------------------------------------------------------ 
PERMISSION DOCUMENTATION ------------------------------------------------------------ 
Produce a complete permission reference. 
For every permission document: 
Permission node 
Description 
Default state 
Recommended role 
Related commands 
Related GUI 
Related feature 
Bypass impact 
Security notes ------------------------------------------------------------ 
PLACEHOLDER DOCUMENTATION ------------------------------------------------------------ 
Produce a complete placeholder reference. 
For every placeholder document: 
Identifier 
Description 
Return value 
Example output 
Scope 
Required module 
Required integration 
Cache behavior 
Unavailable behavior ------------------------------------------------------------ 
API DOCUMENTATION ------------------------------------------------------------ 
Provide: 
JavaDoc 
API usage examples 
Sample extension 
Event examples 
Threading requirements 
Exception behavior 
Versioning policy 
Deprecation policy 
Compatibility policy 
Maven dependency instructions 
Repository instructions ------------------------------------------------------------ 
AUTOMATED TESTING ------------------------------------------------------------ 
Provide: 
Unit tests 
Integration tests 
Regression tests 
Database tests 
Migration tests 
Configuration tests 
Permission tests 
Command tests 
GUI tests 
API tests 
Placeholder tests 
World tests 
Arena lifecycle tests 
Proxy tests 
Redis tests 
CloudNet tests 
Replay tests 
Atlas tests 
Grim integration tests 
Vulcan integration tests 
Party integration tests 
Performance tests 
Load tests 
Failure recovery tests ------------------------------------------------------------ 
GAMEPLAY TEST MATRIX ------------------------------------------------------------ 
Test: 
Solo 
Doubles 
3v3v3v3 
4v4v4v4 
Custom teams 
Standard mode 
Rush mode 
Ultimate mode 
Armed mode 
Voidless mode 
LuckyBlock mode 
BedSteal mode 
Swappage mode 
Adventure mode 
Private games 
Shared-server mode 
Proxy mode 
Rejoin 
Disconnect 
Crash recovery 
World reset 
Map duplication ------------------------------------------------------------ 
PERFORMANCE TESTING ------------------------------------------------------------ 
Benchmark at minimum: 
One active arena 
Ten active arenas 
Forty managed worlds 
Multiple simultaneous world resets 
High shop usage 
High PlaceholderAPI usage 
Replay recording 
Atlas case review 
Large statistics database 
Redis synchronization 
Proxy transfers 
NPC and hologram updates 
Cosmetic particle load 
Quest event load 
The platform must report measurable results. 
Do not use vague statements such as “optimized.” ------------------------------------------------------------ 
GITHUB AND CI/CD ------------------------------------------------------------ 
Provide: 
Clean repository structure 
README 
License files 
Third-party notices 
Maven build 
GitHub Actions 
Clean build workflow 
Unit-test workflow 
Integration-test workflow 
Static-analysis workflow 
Checkstyle 
SpotBugs 
Dependency vulnerability scanning 
Artifact packaging 
Checksum generation 
Release notes 
Version tagging 
Snapshot builds 
Release builds 
Documentation generation ------------------------------------------------------------ 
BUILD OUTPUT ------------------------------------------------------------ 
Produce clearly named artifacts. 
Where modular deployment is used, provide: 
Paper plugin 
Velocity module 
BungeeCord module 
CloudNet module 
API artifact 
Documentation artifact 
Example extension 
Do not package development-only dependencies into production artifacts unnecessarily. ------------------------------------------------------------ 
INSTALLATION VALIDATOR ------------------------------------------------------------ 
Provide a startup installation validator. 
It must detect: 
Wrong Java version 
Wrong Paper version 
Missing required plugin 
Unsupported plugin version 
Conflicting plugin 
Invalid database 
Invalid Redis 
Invalid proxy configuration 
Invalid CloudNet configuration 
Invalid world provider 
Invalid permissions provider 
Invalid configuration 
Duplicate map IDs 
Broken arena references 
Missing migrations 
Unsafe filesystem permissions 
The validator must produce actionable instructions. ------------------------------------------------------------ 
PLUGIN DOCTOR ------------------------------------------------------------ 
Provide an administrative diagnostic tool. 
Plugin Doctor must inspect: 
Server version 
Java version 
Plugin versions 
Integration status 
World status 
Arena status 
Map IDs 
Database health 
Redis health 
Proxy health 
CloudNet health 
Thread queues 
Replay queues 
Memory usage 
TPS 
Loaded chunks 
Entities 
Scheduled tasks 
Placeholder cache 
Configuration errors 
Permission issues 
The report must be exportable. 
Sensitive credentials must never appear in exported reports. ------------------------------------------------------------ 
FINAL DELIVERY REQUIREMENTS ------------------------------------------------------------ 
Codex must deliver: 
Complete source code 
Clean GitHub repository 
Buildable Maven project 
Production JAR files 
Complete default configuration 
Fully commented configuration 
Complete documentation 
Command reference 
Permission reference 
Placeholder reference 
API reference 
Migration tools 
Backup tools 
Automated tests 
CI/CD workflows 
Compatibility matrix 
Performance benchmark report 
Known limitations 
Changelog 
Release notes 
Example configurations 
Example extension 
No TODO 
No FIXME 
No stub 
No mock-only production feature 
No undocumented command 
No undocumented permission 
No undocumented configuration option 
No undocumented placeholder ------------------------------------------------------------ 
FINAL SELF-AUDIT ------------------------------------------------------------ 
Before declaring the project complete, Codex must generate a final compliance report. 
For every Requirement ID include: 
Requirement ID 
Status 
Implementation location 
Configuration location 
Command 
Permission 
GUI 
API 
Placeholder 
Tests 
Documentation 
Performance evidence 
Security evidence 
Known limitations 
Allowed statuses: 
IMPLEMENTED AND VERIFIED 
or: 
TECHNICALLY IMPOSSIBLE WITH APPROVED ALTERNATIVE 
No other status is accepted. 
------------------------------------------------------------ 
ABSOLUTE COMPLETION RULE ------------------------------------------------------------ 
The project is not complete merely because it compiles. 
The project is complete only when: 
All mandatory functionality is implemented. 
All required integrations are implemented. 
All GUIs are implemented. 
All commands are implemented. 
All permissions are implemented. 
All configurations are complete and commented. 
All APIs are documented. 
All placeholders are documented. 
All automated tests pass. 
All supported deployment modes work. 
Performance benchmarks pass. 
Migration and recovery procedures work. 
The final compliance report contains no unresolved mandatory requirement. ------------------------------------------------------------ 
PROJECT GOVERNANCE ------------------------------------------------------------ 
The PRD is the single authoritative source for the project. 
Every implementation decision must originate from the PRD. 
No code may intentionally contradict the PRD. 
If implementation reveals a missing requirement, update the PRD first, then implement. 
Never silently change requirements. 
------------------------------------------------------------ 
PRD OWNERSHIP ------------------------------------------------------------ 
The PRD is considered the contract between the project owner and the implementation. 
Every Requirement ID represents a contractual obligation. 
Removing or changing a Requirement ID requires explicit approval from the project owner. 
Never reinterpret a requirement to simplify implementation. ------------------------------------------------------------ 
PRD STRUCTURE ------------------------------------------------------------ 
Maintain a structured documentation hierarchy. 
Example: 
Volume 
Section 
Chapter 
Requirement 
Sub Requirement 
Appendix 
Reference 
Every Requirement ID must remain stable across future versions whenever possible. ------------------------------------------------------------ 
REQUIREMENT IDS ------------------------------------------------------------ 
Every requirement must contain: 
Requirement ID 
Title 
Description 
Priority 
Category 
Dependencies 
Acceptance Criteria 
Implementation Notes 
Performance Notes 
Security Notes 
Configuration Impact 
Database Impact 
API Impact 
GUI Impact 
Commands 
Permissions 
PlaceholderAPI 
Testing Requirements 
Documentation Requirements 
Migration Requirements 
Status ------------------------------------------------------------ 
REQUIREMENT STATUS ------------------------------------------------------------ 
Allowed states: 
NOT STARTED 
PLANNED 
IN PROGRESS 
IMPLEMENTED 
TESTED 
DOCUMENTED 
VERIFIED 
DEPRECATED 
REMOVED (Only with explicit owner approval) 
Never invent additional status values. ------------------------------------------------------------ 
REQUIREMENT TRACEABILITY ------------------------------------------------------------ 
Every Requirement ID must reference: 
Java Package 
Classes 
Configuration 
Commands 
Permissions 
GUI 
Database Tables 
Redis Structures 
Events 
Public API 
PlaceholderAPI 
Tests 
Documentation 
Migration 
Release Notes 
Never allow orphan requirements. ------------------------------------------------------------ 
CHANGE MANAGEMENT ------------------------------------------------------------ 
Every significant architectural change must include: 
Reason 
Impact 
Affected Modules 
Affected Requirements 
Migration Requirements 
Backward Compatibility 
Performance Impact 
Security Impact 
Testing Impact 
Documentation Update ------------------------------------------------------------ 
DEPENDENCY MANAGEMENT ------------------------------------------------------------ 
Track dependencies between requirements. 
Support: 
Parent Requirements 
Child Requirements 
Blocking Requirements 
Optional Requirements 
Related Requirements 
Mutually Exclusive Requirements ------------------------------------------------------------ 
AUTONOMOUS REQUIREMENT DISCOVERY ------------------------------------------------------------ 
While implementing the project, identify missing enterprise-grade requirements. 
Examples: 
Missing validation 
Missing recovery 
Missing logging 
Missing APIs 
Missing configuration 
Missing testing 
Missing GUI 
Missing commands 
Missing permissions 
Missing PlaceholderAPI values 
If discovered: 
Add the requirement 
Document it 
Implement it 
Test it 
Do not remove or weaken existing requirements. ------------------------------------------------------------ 
RISK MANAGEMENT ------------------------------------------------------------ 
For every subsystem evaluate: 
Complexity 
Performance Risk 
Security Risk 
Scalability Risk 
Compatibility Risk 
Migration Risk 
Maintenance Risk 
Testing Difficulty 
Future Evolution 
Provide mitigation strategies. ------------------------------------------------------------ 
IMPLEMENTATION PHASES ------------------------------------------------------------ 
Every subsystem must follow: 
Analysis 
Architecture 
Specification 
Implementation 
Compilation 
Unit Testing 
Integration Testing 
Performance Validation 
Documentation 
Acceptance Review 
Never skip phases. 
------------------------------------------------------------ 
SELF REVIEW ------------------------------------------------------------ 
After every implementation phase ask: 
Is architecture still clean? 
Is there duplicated logic? 
Can this be modularized further? 
Can performance improve? 
Can configuration improve? 
Can documentation improve? 
Can API improve? 
Can tests improve? 
Can maintainability improve? 
Repeat until no major issue remains. ------------------------------------------------------------ 
TECHNICAL DEBT ------------------------------------------------------------ 
Avoid technical debt whenever reasonably possible. 
If technical debt is intentionally accepted: 
Document it. 
Estimate impact. 
Estimate removal cost. 
Create follow-up requirement. ------------------------------------------------------------ 
BACKWARD COMPATIBILITY ------------------------------------------------------------ 
Never introduce breaking changes silently. 
When unavoidable: 
Document breaking changes. 
Provide migration. 
Provide compatibility layer where practical. 
Update documentation. ------------------------------------------------------------ 
VERSIONING ------------------------------------------------------------ 
Use Semantic Versioning. 
Track: 
Major 
Minor 
Patch 
Configuration Version 
Database Schema Version 
Replay Version 
API Version 
Placeholder Version 
Migration Version ------------------------------------------------------------ 
CODE REVIEW ------------------------------------------------------------ 
Review every subsystem before considering it complete. 
Review: 
Architecture 
Naming 
Readability 
Performance 
Memory 
Thread Safety 
Configuration 
Commands 
Permissions 
GUI 
Documentation 
Tests 
API ------------------------------------------------------------ 
AUTOMATED QUALITY GATES ------------------------------------------------------------ 
Do not consider any subsystem complete unless: 
Compiles 
Tests pass 
Documentation updated 
Configuration updated 
Permissions documented 
Commands documented 
PlaceholderAPI documented 
Performance acceptable 
Acceptance Criteria satisfied ------------------------------------------------------------ 
PROJECT HEALTH 
------------------------------------------------------------ 
Maintain continuous visibility of: 
Build Status 
Test Status 
Coverage 
Open Risks 
Technical Debt 
Known Limitations 
Pending Requirements 
Compatibility Issues 
Performance Regressions 
Migration State ------------------------------------------------------------ 
FINAL RULE ------------------------------------------------------------ 
Never optimize development speed at the expense of software quality. 
The goal is to deliver an enterprise-grade BedWars platform suitable for long-term 
commercial-quality maintenance. ------------------------------------------------------------ 
FINAL DELIVERY PHILOSOPHY ------------------------------------------------------------ 
The objective is NOT to produce code. 
The objective is to deliver a complete enterprise software product. 
The project is considered complete only when: 
Source Code 
Documentation 
Configuration 
GUI 
Commands 
Permissions 
PlaceholderAPI 
Developer APIs 
Tests 
Performance 
Security 
Compatibility 
Migration 
Packaging 
Release 
have all reached production quality. 
Compilation alone is NOT considered completion. ------------------------------------------------------------ 
MANDATORY DELIVERABLES ------------------------------------------------------------ 
Deliver all of the following. 
Source Code 
Maven Project 
GitHub Repository 
README 
LICENSE 
CHANGELOG 
THIRD PARTY NOTICES 
Installation Guide 
Quick Start Guide 
Administrator Guide 
Developer Guide 
Configuration Guide 
Migration Guide 
Performance Guide 
Troubleshooting Guide 
API Reference 
JavaDoc 
Commands Reference 
Permissions Reference 
PlaceholderAPI Reference 
Replay Guide 
Atlas Guide 
Statistics Guide 
Battle Pass Guide 
Quest Guide 
Achievement Guide 
Cosmetics Guide 
Private Games Guide 
Proxy Guide 
Velocity Guide 
Bungee Guide 
CloudNet Guide 
Redis Guide 
Database Guide 
NPC Guide 
World Provider Guide 
Testing Documentation 
CI/CD Documentation 
Architecture Documentation 
Release Notes 
Example Configuration Files 
Example Extensions ------------------------------------------------------------ 
CONFIGURATION DELIVERY ------------------------------------------------------------ 
Every configuration file must be fully commented. 
Every option must include: 
Purpose 
Default Value 
Accepted Values 
Examples 
Dependencies 
Performance Impact 
Reload Support 
Restart Requirement 
Security Considerations 
Compatibility Notes 
Migration Notes 
No undocumented configuration option is allowed. ------------------------------------------------------------ 
COMMAND DELIVERY ------------------------------------------------------------ 
Document every command. 
Include: 
Name 
Aliases 
Syntax 
Arguments 
Examples 
Permission 
Description 
Console Support 
Player Support 
Error Conditions 
Related GUI 
Related Configuration ------------------------------------------------------------ 
PERMISSION DELIVERY ------------------------------------------------------------ 
Document every permission. 
Include: 
Permission Node 
Description 
Default Access 
Recommended Role 
Commands 
GUI 
Feature 
Security Notes ------------------------------------------------------------ 
PLACEHOLDER DELIVERY ------------------------------------------------------------ 
Document every PlaceholderAPI placeholder. 
Include: 
Identifier 
Description 
Return Value 
Example 
Requirements 
Performance Notes ------------------------------------------------------------ 
API DELIVERY ------------------------------------------------------------ 
Every public API must include: 
JavaDoc 
Usage Examples 
Threading Requirements 
Exceptions 
Version Information 
Migration Notes 
Deprecation Policy ------------------------------------------------------------ 
TESTING DELIVERY ------------------------------------------------------------ 
Provide evidence that every subsystem has been tested. 
Tests must include: 
Unit Tests 
Integration Tests 
Regression Tests 
Database Tests 
Redis Tests 
Replay Tests 
Atlas Tests 
Statistics Tests 
PlaceholderAPI Tests 
Proxy Tests 
World Tests 
Arena Tests 
Performance Tests 
Load Tests 
Recovery Tests 
Migration Tests 
Security Tests 
Compatibility Tests ------------------------------------------------------------ 
COMPATIBILITY MATRIX ------------------------------------------------------------ 
Generate a compatibility report for: 
Paper 
Velocity 
BungeeCord 
CloudNet 
Redis 
SQLite 
MySQL 
MariaDB 
PlaceholderAPI 
Vault 
LuckPerms 
ProtocolLib 
WorldEdit 
FAWE 
WorldGuard 
SlimeWorldManager 
Multiverse-Core 
Citizens 
ZNPCs Plus 
DecentHolograms 
AlessioDP Parties 
Grim Anti-Cheat 
Vulcan Anti-Cheat 
ViaVersion 
ViaBackwards 
ViaRewind 
Floodgate 
Geyser 
For each integration specify: 
Supported Version 
Status 
Known Limitations 
Configuration Notes 
Performance Notes ------------------------------------------------------------ 
PERFORMANCE REPORT ------------------------------------------------------------ 
Generate a benchmark report. 
Include: 
Server Startup 
Arena Creation 
Arena Reset 
World Clone 
Map Duplication 
Queue Performance 
Shop Performance 
Replay Recording 
Atlas Review 
Statistics Updates 
Placeholder Performance 
Database Throughput 
Redis Throughput 
Proxy Transfer 
Memory Usage 
CPU Usage 
TPS 
MSPT 
Chunk Usage 
Entity Count 
Thread Count ------------------------------------------------------------ 
SECURITY REPORT ------------------------------------------------------------ 
Document: 
Exploit Protection 
Duplicate Prevention 
Permission Validation 
Packet Validation 
Replay Integrity 
Database Security 
Redis Security 
API Security 
Command Validation 
Configuration Validation 
Migration Validation ------------------------------------------------------------ 
REPOSITORY STRUCTURE ------------------------------------------------------------ 
The repository must remain organized. 
Recommended structure: 
/api 
/core 
/game 
/arena 
/lobby 
/world 
/maps 
/setup 
/shop 
/upgrades 
/generators 
/items 
/quests 
/achievements 
/battlepass 
/cosmetics 
/replay 
/atlas 
/statistics 
/placeholders 
/database 
/redis 
/proxy 
/cloudnet 
/gui 
/commands 
/permissions 
/configuration 
/integrations 
/tests 
/docs 
/examples ------------------------------------------------------------ 
CI/CD ------------------------------------------------------------ 
Provide: 
GitHub Actions 
Build Pipeline 
Static Analysis 
Unit Tests 
Integration Tests 
Packaging 
Release Workflow 
Documentation Generation 
Artifact Generation ------------------------------------------------------------ 
FINAL COMPLIANCE REPORT ------------------------------------------------------------ 
Generate one final report. 
For every Requirement ID include: 
Requirement ID 
Status 
Implementation Location 
Commands 
Permissions 
GUI 
Configuration 
PlaceholderAPI 
API 
Tests 
Documentation 
Performance Verification 
Security Verification 
Compatibility Verification 
Only the following final statuses are allowed: 
IMPLEMENTED AND VERIFIED 
TECHNICALLY IMPOSSIBLE WITH APPROVED ALTERNATIVE 
Nothing else. ------------------------------------------------------------ 
ABSOLUTE FINAL RULE ------------------------------------------------------------ 
Never declare the project complete because: 
"It compiles." 
"It works." 
"The tests pass." 
Completion requires that every mandatory Requirement ID has reached: 
IMPLEMENTED 
TESTED 
DOCUMENTED 
CONFIGURED 
LOCALIZED 
PERMISSIONED 
API EXPOSED 
PLACEHOLDER EXPOSED (where applicable) 
GUI AVAILABLE (where applicable) 
PERFORMANCE VERIFIED 
SECURITY VERIFIED 
COMPATIBILITY VERIFIED 
Only then may the project status become: 
PRODUCTION READY 
------------------------------------------------------------ 
PROJECT OWNER AUTHORITY ------------------------------------------------------------ 
The project owner always has final authority. 
If the project owner requests: 
new functionality 
architectural changes 
additional compatibility 
performance improvements 
documentation improvements 
the PRD must be updated first, 
then implementation may continue. 
The PRD remains the single source of truth throughout the lifetime of the project. ------------------------------------------------------------ 
LONG TERM PRODUCT VISION ------------------------------------------------------------ 
ZartraBedWars must not be designed only for the first public release. 
The platform must be designed to evolve for many years. 
Architecture decisions must prioritize long-term maintainability over short-term 
implementation speed. 
Never design systems that prevent future expansion. ------------------------------------------------------------ 
FUTURE COMPATIBILITY ------------------------------------------------------------ 
Prepare the architecture for future support of: 
Future Minecraft versions 
Future Paper APIs 
Future Java versions 
Future proxy implementations 
Future world providers 
Future NPC providers 
Future hologram providers 
Future databases 
Future cloud providers 
Future replay formats 
Future anticheat providers 
Future PlaceholderAPI changes 
Future GUI systems 
Future BedWars game modes ------------------------------------------------------------ 
PLUGIN ECOSYSTEM ------------------------------------------------------------ 
Design the platform so that it can become the foundation of an ecosystem. 
Support: 
Official Addons 
Third Party Addons 
Community Addons 
Marketplace-ready architecture 
Optional Modules 
Premium Modules 
Open Source Modules 
Enterprise Modules 
------------------------------------------------------------ 
MODULE MARKETPLACE ------------------------------------------------------------ 
The architecture should support future module installation without modifying the core. 
Possible future modules: 
Tournament System 
Guild System 
Clan Wars 
Ranked BedWars 
Economy Expansion 
Seasonal Events 
Custom NPC Packs 
Custom Shop Packs 
Custom Quest Packs 
Custom Cosmetic Packs 
Custom Replay Extensions 
Custom Atlas Providers 
Custom World Providers 
Custom Statistics Providers 
Custom Matchmaking Providers ------------------------------------------------------------ 
PLUGIN SDK ------------------------------------------------------------ 
Prepare an SDK for developers. 
The SDK should allow developers to create: 
New Game Modes 
New Shops 
New Upgrade Types 
New Generator Types 
New Quest Types 
New Achievement Types 
New Battle Pass Rewards 
New Cosmetics 
New Replay Events 
New Atlas Integrations 
New Statistics 
New Placeholders 
New GUI Pages 
New Commands 
New NPC Providers 
New World Providers 
New Matchmaking Algorithms ------------------------------------------------------------ 
PLUGIN MARKETPLACE API ------------------------------------------------------------ 
Design APIs so future marketplaces can validate: 
Compatibility 
Version 
Dependencies 
Required APIs 
Permissions 
Configuration 
Supported Minecraft Versions 
Supported ZartraBedWars Versions ------------------------------------------------------------ 
MODULE ISOLATION ------------------------------------------------------------ 
Future modules must never require editing the core source code. 
Every module must communicate only through documented APIs. ------------------------------------------------------------ 
DEPRECATION POLICY ------------------------------------------------------------ 
Never remove APIs immediately. 
Mark them Deprecated. 
Document replacement. 
Maintain backward compatibility for a reasonable period. 
Generate migration reports. ------------------------------------------------------------ 
MIGRATION ASSISTANT ------------------------------------------------------------ 
Create architecture for future migration assistants. 
Examples: 
Configuration Migration 
Database Migration 
Replay Migration 
Statistics Migration 
Placeholder Migration 
Permission Migration 
World Migration 
Arena Migration ------------------------------------------------------------ 
PLUGIN DOCTOR EVOLUTION ------------------------------------------------------------ 
The Plugin Doctor should be extensible. 
Future checks should be installable through providers. ------------------------------------------------------------ 
PERFORMANCE EVOLUTION ------------------------------------------------------------ 
Prepare architecture for: 
Future async improvements 
Future scheduler improvements 
Future replay compression 
Future cache systems 
Future Redis providers 
Future distributed databases ------------------------------------------------------------ 
AI SUPPORT ------------------------------------------------------------ 
Design internal APIs that may later allow AI-powered assistants. 
Examples: 
Automatic configuration validation 
Automatic diagnostics 
Automatic optimization suggestions 
Automatic balancing 
Automatic replay summaries 
Automatic suspicious behavior summaries 
Automatic documentation generation 
Automatic migration suggestions ------------------------------------------------------------ 
ENTERPRISE ROADMAP ------------------------------------------------------------ 
Maintain an internal roadmap. 
Every major feature should contain: 
Current Status 
Planned Improvements 
Known Limitations 
Future APIs 
Future GUI 
Future Commands 
Future Permissions 
Future Placeholders ------------------------------------------------------------ 
QUALITY PRINCIPLE ------------------------------------------------------------ 
The project must never become harder to maintain as it grows. 
Every new feature should reduce complexity where possible instead of increasing it. ------------------------------------------------------------ 
FINAL EVOLUTION RULE ------------------------------------------------------------ 
Whenever a future feature can be implemented through existing APIs and extension points 
instead of modifying the core, 
that approach must be preferred. 
The core should remain as stable as possible while the ecosystem evolves around it. 
------------------------------------------------------------ 
THIS SECTION HAS THE HIGHEST PRIORITY ------------------------------------------------------------ 
This section supersedes generic instructions whenever a conflict exists. 
No previous functionality is removed. 
This section only clarifies mandatory implementation standards. ------------------------------------------------------------ 
REQUIREMENT PRIORITIES ------------------------------------------------------------ 
Every Requirement ID must contain one of the following priorities. 
CRITICAL MUST 
Required for the platform to function correctly. 
The project may never be considered production-ready if one Critical MUST requirement is 
missing. 
Examples: 
Core Game Engine 
Arena Lifecycle 
Database 
Configuration 
Replay 
Statistics 
Commands 
Permissions 
API 
Documentation 
Testing 
MUST 
Required before stable release. 
SHOULD 
Strongly recommended. 
May only be postponed with documented justification. 
MAY 
Optional improvement. 
May be implemented after all higher priorities. ------------------------------------------------------------ 
DEPENDENCY DECLARATION ------------------------------------------------------------ 
Every subsystem must explicitly declare: 
Depends On 
Required By 
Optional Integrations 
External Providers 
Configuration Files 
Database Tables 
Redis Usage 
Placeholder Groups 
GUI 
Commands 
Permissions 
Public API 
Events 
No subsystem may have hidden dependencies. 
------------------------------------------------------------ 
DEFINITION OF DONE ------------------------------------------------------------ 
A feature is COMPLETE only when ALL applicable conditions are satisfied. 
Architecture completed 
Implementation completed 
Compilation successful 
Unit tests passed 
Integration tests passed 
Regression tests passed 
Performance validated 
Security validated 
Configuration completed 
Configuration commented 
Commands completed 
Permissions completed 
GUI completed 
PlaceholderAPI completed 
Public API completed 
JavaDoc completed 
Documentation completed 
Migration completed 
Compatibility verified 
Acceptance Criteria verified 
Only then may the Requirement ID become: 
IMPLEMENTED AND VERIFIED ------------------------------------------------------------ 
USER EXPERIENCE STANDARD ------------------------------------------------------------ 
Every GUI must provide a consistent experience. 
Requirements: 
Consistent layout 
Consistent navigation 
Consistent Back button 
Consistent Close button 
Consistent confirmation dialogs 
Consistent sounds 
Consistent animations 
Consistent icon philosophy 
Consistent color usage 
Consistent error messages 
Consistent loading indicators 
Consistent pagination 
Consistent search 
Consistent filtering 
Consistent permissions 
No subsystem may introduce an inconsistent interface. ------------------------------------------------------------ 
ACCESSIBILITY ------------------------------------------------------------ 
Support: 
Colorblind-friendly design where practical 
Readable text 
Scalable GUI layouts 
Keyboard-friendly inventory navigation where possible 
Bedrock-compatible alternatives where appropriate ------------------------------------------------------------ 
ARCHITECTURE DECISION RECORD ------------------------------------------------------------ 
Every major architectural decision must create an ADR. 
Each ADR must include: 
Decision 
Reason 
Alternatives 
Trade-offs 
Performance impact 
Compatibility impact 
Migration impact 
Future impact ------------------------------------------------------------ 
OBSERVABILITY ------------------------------------------------------------ 
Provide native observability. 
Include: 
Plugin Doctor 
Health Dashboard 
Performance Dashboard 
Memory Dashboard 
Thread Dashboard 
Replay Queue Monitor 
Redis Monitor 
Database Monitor 
Proxy Monitor 
CloudNet Monitor 
Arena Health 
World Health 
Configuration Validator 
Dependency Validator 
Metrics 
Benchmark Reports 
Slow Operation Detection 
Debug Reports ------------------------------------------------------------ 
SECRET MANAGEMENT ------------------------------------------------------------ 
Never expose: 
Passwords 
Database credentials 
Redis credentials 
Tokens 
Secrets 
API Keys 
Private URLs 
Sensitive information must never appear inside: 
Logs 
Exports 
Diagnostics 
Crash Reports ------------------------------------------------------------ 
SELF IMPROVEMENT ------------------------------------------------------------ 
If an implementation can clearly be improved without reducing compatibility or functionality, 
prefer the better architecture. 
Never choose the shortest implementation simply because it is easier. ------------------------------------------------------------ 
NO DUPLICATION ------------------------------------------------------------ 
Never duplicate: 
Business Logic 
Database Logic 
Configuration Logic 
Permission Logic 
Placeholder Logic 
GUI Logic 
Statistics Logic 
Replay Logic 
Use reusable services. ------------------------------------------------------------ 
ERROR HANDLING ------------------------------------------------------------ 
Every failure must: 
be logged 
be recoverable where possible 
produce meaningful messages 
avoid data corruption 
avoid server crashes 
avoid silent failures ------------------------------------------------------------ 
LONG TERM MAINTAINABILITY ------------------------------------------------------------ 
Always prefer: 
Maintainability 
Readability 
Modularity 
Configurability 
Extensibility 
Testability 
Documentation 
over reducing file count or implementation effort. ------------------------------------------------------------ 
FINAL IMPLEMENTATION RULE ------------------------------------------------------------ 
If two different implementations satisfy the same Requirement ID, 
always choose the implementation that provides: 
better architecture 
higher maintainability 
higher scalability 
higher configurability 
better documentation 
better API design 
better testing 
better future compatibility 
even if implementation requires significantly more work. ------------------------------------------------------------ 
ABSOLUTE FINAL RULE ------------------------------------------------------------ 
The objective of ZartraBedWars is not to become another BedWars plugin. 
The objective is to become the reference enterprise BedWars platform for modern Minecraft 
servers. 
Every design decision must move the project toward that. 
