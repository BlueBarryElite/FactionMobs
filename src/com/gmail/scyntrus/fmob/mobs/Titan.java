package com.gmail.scyntrus.fmob.mobs;

import java.util.LinkedHashSet;

import net.minecraft.server.v1_9_R2.AttributeInstance;
import net.minecraft.server.v1_9_R2.DamageSource;
import net.minecraft.server.v1_9_R2.Entity;
import net.minecraft.server.v1_9_R2.EntityCreature;
import net.minecraft.server.v1_9_R2.EntityHuman;
import net.minecraft.server.v1_9_R2.EntityIronGolem;
import net.minecraft.server.v1_9_R2.EntityLiving;
import net.minecraft.server.v1_9_R2.EntityPlayer;
import net.minecraft.server.v1_9_R2.EntityProjectile;
import net.minecraft.server.v1_9_R2.EnumItemSlot;
import net.minecraft.server.v1_9_R2.EnumMonsterType;
import net.minecraft.server.v1_9_R2.GenericAttributes;
import net.minecraft.server.v1_9_R2.MathHelper;
import net.minecraft.server.v1_9_R2.NBTTagCompound;
import net.minecraft.server.v1_9_R2.PathfinderGoalFloat;
import net.minecraft.server.v1_9_R2.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_9_R2.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_9_R2.PathfinderGoalMoveTowardsTarget;
import net.minecraft.server.v1_9_R2.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_9_R2.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_9_R2.SoundEffects;
import net.minecraft.server.v1_9_R2.World;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.gmail.scyntrus.fmob.ErrorManager;
import com.gmail.scyntrus.fmob.FactionMob;
import com.gmail.scyntrus.fmob.FactionMobs;
import com.gmail.scyntrus.fmob.ReflectionManager;
import com.gmail.scyntrus.fmob.Utils;
import com.gmail.scyntrus.ifactions.Faction;
import com.gmail.scyntrus.ifactions.FactionsManager;

public class Titan extends EntityIronGolem implements FactionMob {

    public static final String typeName = "Titan";
    public Location spawnLoc = null;
    public Faction faction = null;
    public String factionName = "";
    public EntityLiving attackedBy = null;
    public EntityLiving target = null;
    public static float maxHp = 40;
    public static Boolean enabled = true;
    public static double powerCost = 0;
    public static double moneyCost = 1;
    public static double range = 16;
    public static double damage = 0;
    public static int drops = 0;
    private int retargetTime = 0;
    private double moveSpeed;

    public double poiX=0, poiY=0, poiZ=0;
    public String order = "poi";

    public Titan(World world) {
        super(world);
        this.forceDie();
    }

    public Titan(Location spawnLoc, Faction faction) {
        super(((CraftWorld) spawnLoc.getWorld()).getHandle());
        this.setSpawn(spawnLoc);
        this.setFaction(faction);
        this.persistent = true;
        this.fireProof = false;
        this.canPickUpLoot = false;
        this.moveSpeed = FactionMobs.mobSpeed;
        getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(this.moveSpeed);
        getAttributeInstance(GenericAttributes.maxHealth).setValue(maxHp);
        this.setHealth(maxHp);
        this.P = 1.5F; // TODO: Update name on version change (E: jump height)
        this.retargetTime = FactionMobs.random.nextInt(40);

        if (ReflectionManager.good_Navigation_Distance) {
            try {
                AttributeInstance e = (AttributeInstance) ReflectionManager.navigation_Distance.get(this.getNavigation());
                e.setValue(FactionMobs.mobNavRange);
            } catch (Exception e) {
                ErrorManager.handleError(e);
            }
        }
        if (ReflectionManager.good_PathfinderGoalSelector_GoalSet) {
            try {
                @SuppressWarnings("rawtypes")
                LinkedHashSet tempSet1 = (LinkedHashSet) ReflectionManager.pathfinderGoalSelector_GoalSet.get(this.goalSelector);
                tempSet1.clear();
                @SuppressWarnings("rawtypes")
                LinkedHashSet tempSet2 = (LinkedHashSet) ReflectionManager.pathfinderGoalSelector_GoalSet.get(this.targetSelector);
                tempSet2.clear();
            } catch (Exception e) {
                ErrorManager.handleError(e);
            }
        }

        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, new PathfinderGoalMeleeAttack(this, 1.0, true));
        this.goalSelector.a(3, new PathfinderGoalMoveTowardsTarget(this, 1.0, (float) range));
        this.goalSelector.a(4, new PathfinderGoalRandomStroll(this, 1.0));
        this.goalSelector.a(5, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(5, new PathfinderGoalRandomLookaround(this));
        this.getBukkitEntity().setMetadata("CustomEntity", new FixedMetadataValue(FactionMobs.instance, true));
        this.getBukkitEntity().setMetadata("FactionMob", new FixedMetadataValue(FactionMobs.instance, true));
    }

    @Override
    public void n() { //TODO: Update name on version change (E: entity tick)
        super.n();
        if (this.inWater) {
            this.motY += .1;
        }
        if (--retargetTime < 0) {
            retargetTime = FactionMobs.responseTime;
            if (this.getGoalTarget() == null || !this.getGoalTarget().isAlive()) {
                this.findTarget();
            } else {
                double dist = Utils.dist3D(this.locX, this.getGoalTarget().locX, this.locY, this.getGoalTarget().locY, this.locZ, this.getGoalTarget().locZ);
                if (dist > range) {
                    this.findTarget();
                } else if (dist > 3) {
                    this.findCloserTarget();
                }
            }
            if (this.getGoalTarget() == null) {
                if (this.order.equals("home") || this.order == null || this.order.equals("")) {
                    this.getNavigation().a(this.spawnLoc.getX(), this.spawnLoc.getY(), this.spawnLoc.getZ(), 1.0);
                    this.order = "home";
                    return;
                } else if (this.order.equals("poi")) {
                    this.getNavigation().a(this.poiX, this.poiY, this.poiZ, 1.0);
                    return;
                } else if (this.order.equals("wander")) {
                    return;
                } else if (this.order.equals("phome")) {
                    this.getNavigation().a(this.spawnLoc.getX(), this.spawnLoc.getY(), this.spawnLoc.getZ(), FactionMobs.mobPatrolSpeed);
                    if (Utils.dist3D(this.locX,this.spawnLoc.getX(),this.locY,this.spawnLoc.getY(),this.locZ,this.spawnLoc.getZ()) < 2) {
                        this.order = "ppoi";
                    }
                    return;
                } else if (this.order.equals("ppoi")) {
                    this.getNavigation().a(poiX, poiY, poiZ, FactionMobs.mobPatrolSpeed);
                    if (Utils.dist3D(this.locX,this.poiX,this.locY,this.poiY,this.locZ,this.poiZ) < 2.5) {
                        this.order = "phome";
                    }
                    return;
                } else if (this.order.equals("path")) {
                    this.getNavigation().a(poiX, poiY, poiZ, 1.0);
                    if (Utils.dist3D(this.locX,this.poiX,this.locY,this.poiY,this.locZ,this.poiZ) < 2.5) {
                        this.order = "home";
                    }
                    return;
                }
            }
        }
        return;
    }

    private void setSpawn(Location loc) {
        spawnLoc = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
        this.setPosition(loc.getX(), loc.getY(), loc.getZ());
        this.setPoi(loc.getX(),loc.getY(),loc.getZ());
        this.order = "home";
    }

    @Override
    public EntityLiving findCloserTarget() {
        if (this.attackedBy != null) {
            if (this.attackedBy.isAlive()
                    && this.attackedBy.world.getWorldData().getName().equals(this.world.getWorldData().getName())
                    && Utils.FactionCheck(this.attackedBy, this.faction) < 1) {
                double dist = Utils.dist3D(this.locX, this.attackedBy.locX, this.locY, this.attackedBy.locY, this.locZ, this.attackedBy.locZ);
                if (dist < 16) {
                    this.setTarget(this.attackedBy);
                    return this.attackedBy;
                } else if (dist > 32) {
                    this.attackedBy = null;
                }
            } else {
                this.attackedBy = null;
            }
        }
        EntityLiving e = Utils.optimizedTargetSearch(this, 1.5);
        if (e != null)
            this.setTarget(e);
        return e;
    }

    @Override
    public void findTarget() {
        this.setTarget(Utils.optimizedTargetSearch(this, range));
        return;
    }

    @Override
    public boolean damageEntity(DamageSource damagesource, float i) {
        boolean out = super.damageEntity(damagesource, i);
        if (!out)
            return out;
        EntityLiving damager;
        if (damagesource.getEntity() instanceof EntityLiving) {
            damager = (EntityLiving) damagesource.getEntity();
        } else if (damagesource.getEntity() instanceof EntityProjectile) {
            damager = ((EntityProjectile) damagesource.getEntity()).getShooter();
        } else {
            return out;
        }
        switch (Utils.FactionCheck(damager, this.faction)) {
            case 1:
                this.findTarget();
                if (damager instanceof EntityPlayer) {
                    this.lastDamageByPlayerTime = 0;
                }
                break;
            case 0:
            case -1:
                if (damager instanceof EntityLiving) {
                    this.attackedBy = damager;
                    this.setTarget(damager);
                } else if (damagesource.getEntity() instanceof EntityProjectile) {
                    EntityProjectile p = (EntityProjectile) damagesource.getEntity();
                    this.attackedBy = p.getShooter();
                    this.setTarget(p.getShooter());
                } else {
                    this.findTarget();
                }
                break;
        }
        return out;
    }

    @Override
    public boolean canSpawn() {
        return true;
    }

    @Override
    public Faction getFaction() {
        if (this.faction == null) {
            this.setFaction(FactionsManager.getFactionByName(factionName));
        }
        if (this.faction == null) {
            this.forceDie();
            System.out.println("[Error] Found and removed factionless faction mob");
        }
        return this.faction;
    }

    @Override
    public void setFaction(Faction faction) {
        if (faction == null) return;
        this.faction = faction;
        if (faction.isNone()) this.forceDie();
        this.factionName = faction.getName();
        if (FactionMobs.displayMobFaction) {
            this.setCustomName(ChatColor.YELLOW + this.factionName + " " + typeName);
            this.setCustomNameVisible(true);
        }
    }

    @Override
    public void setTarget(EntityLiving entity) {
        this.target = entity;
        if (entity instanceof EntityLiving) {
            this.setGoalTarget(entity);
        } else if (entity == null) {
            this.setGoalTarget(null);
        }
        if (this.getGoalTarget() != null && !this.getGoalTarget().isAlive()) {
            this.setGoalTarget(null);
        }
    }

    @Override
    public boolean setGoalTarget(EntityLiving entityliving, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        if (this.target instanceof EntityLiving && this.target.isAlive()) {
            super.setGoalTarget(this.target, EntityTargetEvent.TargetReason.CUSTOM, false);
        } else {
            super.setGoalTarget(null, EntityTargetEvent.TargetReason.CUSTOM, false);
        }
        return true;
    }

    @Override
    public void updateMob() {
        this.setFaction(FactionsManager.getFactionByName(factionName));
        if (this.faction == null || this.faction.isNone()) {
            this.forceDie();
            return;
        }
        if (this.target instanceof EntityLiving && this.target.isAlive()) {
            this.setGoalTarget(this.target);
        } else {
            this.findTarget();
        }
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public Location getSpawn() {
        return this.spawnLoc;
    }

    @Override
    public double getlocX() {
        return this.locX;
    }

    @Override
    public double getlocY() {
        return this.locY;
    }

    @Override
    public double getlocZ() {
        return this.locZ;
    }

    @Override
    public Boolean getEnabled() {
        return enabled;
    }

    @Override
    public double getPowerCost() {
        return powerCost;
    }

    @Override
    public double getMoneyCost() {
        return moneyCost;
    }

    @Override
    public boolean isTypeNotPersistent() {
        return false;
    }

    @Override
    public double getPoiX() {
        return this.poiX;
    }

    @Override
    public double getPoiY() {
        return this.poiY;
    }

    @Override
    public double getPoiZ() {
        return this.poiZ;
    }

    @Override
    public void setOrder(String order) {
        this.order = order;
    }

    @Override
    public void setPoi(double x, double y, double z) {
        this.poiX = x;
        this.poiY = y;
        this.poiZ = z;
    }

    @Override
    public String getOrder() {
        return this.order;
    }

    @Override
    public EntityCreature getEntity() {
        return this;
    }

    @Override
    public String getFactionName() {
        if (this.factionName == null) this.factionName = "";
        return this.factionName;
    }

    @Override
    public void die() {
        if (this.getHealth() <= 0) {
            super.die();
            this.setHealth(0);
            this.setSlot(EnumItemSlot.CHEST, null);
            this.setSlot(EnumItemSlot.FEET, null);
            this.setSlot(EnumItemSlot.HEAD, null);
            this.setSlot(EnumItemSlot.LEGS, null);
            this.setSlot(EnumItemSlot.MAINHAND, null);
            this.setSlot(EnumItemSlot.OFFHAND, null);
            if (FactionMobs.mobList.contains(this)) {
                FactionMobs.mobList.remove(this);
            }
        }
    }

    @Override
    public void forceDie() {
        this.setHealth(0);
        this.die();
    }

    @Override
    public boolean B(Entity entity) { //TODO: Update name on version change (E: EntityIronGolem attack)
        if (damage>0) {
            this.world.broadcastEntityEffect(this, (byte)4);
            boolean flag = entity.damageEntity(DamageSource.mobAttack(this), (float) damage);
            if (flag) {
                entity.motY += 0.4D;
            }
            a( //TODO: Update name on version change (E: play SoundEffect)
                    SoundEffects.cH, 1.0F, 1.0F); //TODO: Update name on version change (E: entity.irongolem.attack sound)
            return flag;
        } else {
            return super.B(entity); //TODO: Update name on version change (E: EntityIronGolem attack)
        }
    }

    @Override
    public boolean c(NBTTagCompound nbttagcompound) { //TODO: Update name on version change (E: save data check)
        return false;
    }

    @Override
    public boolean d(NBTTagCompound nbttagcompound) { //TODO: Update name on version change (E: save data check)
        return false;
    }

    @Override
    public void f(NBTTagCompound nbttagcompound) {
        this.die();
    }

    @Override
    public void clearAttackedBy() {
        if (this.target == this.attackedBy) {
            this.setTarget(null);
        }
        this.attackedBy = null;
    }

    @Override
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.UNDEFINED;
    }

    @Override
    public int getDrops() {
        return drops;
    }

    @Override
    public boolean softAgro(EntityLiving entity) {
        if (this.attackedBy == null) {
            this.attackedBy = entity;
            this.setTarget(entity);
            return true;
        }
        return false;
    }

    @Override
    public void setHealth(float f) {
        this.datawatcher.set(HEALTH, Float.valueOf(MathHelper.a(f, 0.0F, maxHp)));
    }

    @Override
    public void m() { //TODO: Update name on version change (E: tick)
        if (this.getHealth() > 0) {
            this.dead = false;
        }
        this.ak = false; //TODO: Update name on version change (E: allow portal)
        super.m();
    }
    
}
