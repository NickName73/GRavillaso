package nitis.nickname73.Gravillaso.content.Blocks;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.scene.ui.layout.Scl;
import arc.struct.EnumSet;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.logic.Ranged;
import mindustry.type.ItemStack;
import mindustry.ui.Fonts;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import nitis.nickname73.Gravillaso.Extra.BuildingTarget;
import nitis.nickname73.Gravillaso.Extra.ItemTarget;
import nitis.nickname73.Gravillaso.Extra.NiTiSON;
import nitis.nickname73.Gravillaso.Extra.NoPoint;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class Distributor extends Block{
    public float range = 80f;
    public int linksAmount = 10;

    public int reloadTime = 10;

    public Effect outputEffect = Fx.itemTransfer;
    public Effect inputEffect = Fx.dropItem;
    public Effect successStoleEffect = Fx.smeltsmoke;

    public Distributor(String name){
        super(name);
        solid = true;
        consumes.power(1f);
        configurable = true;
        saveConfig = true;
        update = true;
        noUpdateDisabled = true;
        flags = EnumSet.of(BlockFlag.allLogic);
    }

    @Override
    public void init(){
        if(linksAmount <= 0){
            linksAmount = 1;
        }
        super.init();
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.placing);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.shootRange, range / tilesize, StatUnit.blocks);
        stats.add(Stat.damage, linksAmount, StatUnit.blocks);
    }

    public class DistributorBuild extends Building implements Ranged{
        public boolean inExist = false,outExist = false;
        public int linkAmount = 0;

        public int reloadActiveTime = 0;
        public boolean reloaded = false;

        public Seq<BuildingTarget> selected = new Seq<>();
        private Seq<NoPoint> selectedPoints = new Seq<>();

        private Seq<ItemTarget> itemTargetToGet = new Seq<>();
        private Seq<ItemTarget> itemTargetToUse = new Seq<>();

        @Override
        public float range(){
            return range;
        }
        public float realRange(){
            return range/8;
        }

        public void drawConfigure(){
            Draw.color(Pal.reactorPurple);
            Drawf.dashCircle(x, y, range, this.enabled ? team.color : Color.gray );
            Lines.stroke(1.0F);
            Lines.square(this.x, this.y, (float)(this.block.size * 8) / 2.0F + 1.0F);
            for (BuildingTarget target : selected){
                Draw.color(Pal.remove);
                if(target.getter){
                    Draw.color(Pal.accent);
                }
                if(target.sender){
                    Draw.color(Color.green);
                }
                if(target.getter && target.sender){
                    Draw.color(Pal.lightishGray);
                }
                Lines.square(target.x(), target.y(), (target.block.size * tilesize) / 2.0F + 1.0F);
            }
            Draw.reset();
        }

        @Override
        public boolean onConfigureTileTapped(Building other){
            if(this.team != other.team){
                return false;
            }
            if(this == other) { //Click
                deselect();
            }
            //Miss click
            if(this != other) if (other == null) {
                deselect();
            } else { // Not null
                boolean del = false;
                for (BuildingTarget target : selected) {
                    if (target.target == other) { // Unselect
                        untagBuild(target);
                        del = true;
                    }
                }
                // Select
                if (!del && linkAmount < linksAmount) {
                    if(inRange(other)){
                        tagBuild(other);
                    }
                }
            }
            return super.onConfigureTileTapped(other);
        }

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, range, team.color);
        }
        public boolean inRange(Tile tile){
            return Mathf.dst(x,y,tile.x,tile.y)/8f <= realRange();
        }
        public boolean inRange(Building building){
            return Mathf.dst(x,y,building.x,building.y)/8f <= realRange();
        }
        public boolean inRange(Unit unit){
            return Mathf.dst(x,y,unit.x,unit.y)/8f <= realRange();
        }
        @Override
        public void draw(){
            super.draw();
            if(!this.enabled){
                Draw.color(Color.gray);
            }
            //Draw.rect(topRegion, this.x, this.y, this.block.rotate ? this.rotdeg() : 0.0F);
            Draw.reset();
            Font font = Fonts.outline;
            font.getData().setScale(0.3f / Scl.scl(1.f));
            font.setColor(Color.purple);
            font.draw(linkAmount + "/"+ linksAmount, x,y);
            font.getData().setScale(1f);
        }
        @Override
        public void update(){
            if(selectedPoints.size > 0){
                for (NoPoint point:
                        selectedPoints) {
                    tagBuild( world.build( point.X,point.Y ) );
                }
                selectedPoints.clear();
            }
            if(!reloaded){
                if(reloadActiveTime < reloadTime){
                    reloadActiveTime += 1;
                }
                if(reloadActiveTime >= reloadTime){
                    reloaded = true;
                }
            }
            for(BuildingTarget slot : selected){
                if(slot.target != world.build(slot.target.tileX(),slot.target.tileY())){
                    untagBuild(slot);
                }
            }
            if(this.enabled && reloaded){
                itemTargetToUse.clear();
                itemTargetToGet.clear();
                for (BuildingTarget target:
                     selected) {
                    if(target.getter){
                        BaseDialog dialog = new BaseDialog("New item target");
                        for (ItemStack itm:
                             target.consume.items) {
                            itemTargetToUse.add( new ItemTarget(target.target,itm.item));
                            dialog.cont.add(itm.item.name);
                        }
                        dialog.addCloseButton();
                        dialog.show();
                    }
                    if(target.sender){
                        itemTargetToGet.add(new ItemTarget(target.target,target.output));
                    }
                }
            }
            for (ItemTarget user: itemTargetToUse) {
                for (ItemTarget getter: itemTargetToGet) {
                    if(user.target != getter.target){ // checks what blocks are different
                        if(user.target.acceptItem(getter.target,getter.item)){ // checks item can enter
                            getter.target.removeStack(getter.item,1); // removes if used
                            outputEffect.at(getter.target.x,getter.target.y);
                            inputEffect.at(user.target.x,user.target.y);
                            successStoleEffect.at(this.x,this.y);
                        }else{ //item not enter

                        }
                    }
                }
            }
        }
        public boolean tagBuild(Building building){
            return tagBuild(new BuildingTarget(building));
        }
        public boolean tagBuild(BuildingTarget buildingTarget){
            if(linkAmount >= linksAmount){
                return false;
            }else{
                selected.add(buildingTarget);
                linkAmount += 1;
                return true;
            }
        }
        public void untagBuild(Building building){
            untagBuild(new BuildingTarget(building));
        }
        public void untagBuild(BuildingTarget buildingTarget){
            selected.remove(buildingTarget);
            linkAmount -= 1;
        }
        public Seq<BuildingTarget> getSelected(){
            return selected;
        }
        @Override
        public BlockStatus status() {
            if(itemTargetToGet.size == 0){
                return BlockStatus.noInput;
            }
            if(itemTargetToUse.size == 0){
                return BlockStatus.noOutput;
            }
            return BlockStatus.active;
        }

        @Override
        public boolean shouldConsume() {
            return this.enabled;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            String parsedIntArray = NiTiSON.parseToString(NiTiSON.parseToIntArray(selected));
            String selectedSave = "N";
            if (parsedIntArray.toCharArray().length != 0) {
                selectedSave = parsedIntArray;
            }
            write.str(selectedSave);
        }
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            String number = read.str();
            if(number.equals("N")){
            }else{
                Integer[] numbers = NiTiSON.parseToIntArray(number);
                int constructIndex = 0;
                NoPoint[] points = new NoPoint[numbers.length/2];
                for(int i = 0; i < numbers.length; i++){
                    if( i % 2 == 0 ){
                        int x = numbers[constructIndex * 2];
                        points[constructIndex] = new NoPoint(x,0);
                    }else{
                        int y = numbers[constructIndex * 2 + 1];
                        points[constructIndex].Y = y;
                        constructIndex += 1;
                    }
                }
                for (NoPoint point:
                        points) {
                    selectedPoints.add(point);
                }
            }
        }
    }
}