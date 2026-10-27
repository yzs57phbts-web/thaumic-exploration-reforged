package thaumicexploration;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.*;

/** Local map marker for an already learned entry from another Thaumonomicon tab. */
public final class ProxyResearchItem extends ResearchItem {
    private final String original;
    public ProxyResearchItem(String key,String original,int col,int row){
        super(key,"TX",new AspectList(),col,row,1,TcStack.NONE);this.original=original;
        setStub();setHidden();setParentsHidden(original);
    }
    private ResearchItem source(){return ResearchCategories.getResearch(original);}
    @Override public TcStack iconItem(){return source().iconItem();}
    @Override public ResourceLocation iconTexture(){return source().iconTexture();}
    @Override public String nameKey(){return source().nameKey();}
    @Override public String textKey(){return source().textKey();}
    @Override public List<ResearchPage> pages(){return source().pages();}
    @Override public boolean isSecondary(){return source().isSecondary();}
}
