package subham.simpleapp;

import java.util.Vector;

class menuNode{
    private int id;
    public int top, bottom, height;
    private boolean hidden;
    private String name;
    private int childCount;
    private Vector<menuNode> children;
    public menuNode(int id, String name){
        this.id = id;
        this.name = new String(name);
        hidden = false;
        children = new Vector<>();
        childCount = 0;
    }
    public void addChildren(String names[]){
        for(int i=0;i<names.length;i++)
            this.addChild(new menuNode(childCount+i,names[i]));
    }
    public void addChild(menuNode childMenu){
        children.add(childMenu);
        childCount++;
    }
    public void toggleHidden(boolean hidden){
        this.hidden = hidden;
    }
    public boolean isHidden(){
        return hidden;
    }
    public int getId(){
        return id;
    }
    public int getIdByName(String child_name){
        for(int i=0; i<this.childCount;i++){
            if(children.get(i).getName().equals(child_name)) return i*10+id;
            else {
                int t = children.get(i).getIdByName(child_name);
                if(t!=-1) return t*10+id;
            }
        }
        return -1;
    }
    public menuNode getChildByName(String child_name){
        for(int i=0; i<this.childCount;i++){
            if(children.get(i).getName().equals(child_name)) return children.get(i);
            else return children.get(i).getChildByName(child_name);
        }
        return null;
    }
    public String getNameByID(int id){
        return getChild(id).getName();
    }
    public String getName(){
        return name;
    }
    public int getChildCount() {
        return childCount;
    }
    public menuNode getChild(int id){
        if( id%10 == this.id) {
            int i = (id - this.id) / 10;
            if (i < 10) return children.get(i);
            else return children.get(i % 10).getChild(i);
        }
        return null;
    }
}