package subham.kudoku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import java.util.Random;
import java.util.Vector;


class region{
        public enum inner_type {ADDED, REMOVED}
        int groupID;
        int color;
        Vector<Integer> added;
        Vector<Integer> removed;
        Vector<Point> foreigner;	//for foreigners, f.x stores groupID, f.y stores cellID
        protected void init(int gID, int inner, inner_type type, Vector<Integer> foreign){
            groupID = gID;
            if(type == inner_type.ADDED) added = new Vector<>();
            else removed = new Vector<>();
            while(inner>0){
                if(type == inner_type.ADDED) added.add(inner%10);
                else removed.add(inner%10-1);
                inner/=10;
            }
            if(foreign!=null) {
                foreigner = new Vector<>();
                for(int i=0; i< foreign.size(); i++){
                    int num = foreign.get(i);
                    gID = num%10;
                    num/=10;
                    while(num>0) {
                        foreigner.add(new Point (gID-1,num%10-1));
                        num/= 10;
                    }
                }
            }
        }
    }
class cell{
        int value = 0;
        private int last, last2;
        boolean values[];
        public cell(){
            //TODO: use mask
            values = new boolean[9];
            for (int i = 0; i < 9; i++) {
                values[i]=true;
            }
            last2 = 0;
            last = 0;
        }
        public boolean fill(int i/*0-9*/){  //accepted values are 0-9
            if(value == i) return true;     //check if value already exists
            else if(i>=0 && i<10) {
                //check if value is allowed
                if(i==0) {                  //zero value implies empty cell
                    last2 = last;
                    last = value;
                    value = 0;              //so while it is accepted as a cell value
                }
                else if(this.values[i-1]){
                    last2 = last;
                    last = value;
                    value = i;              //its not included in set of possible values[]
                    for (int j = 1; j < 10; j++) {
                        if(j!=i) values[j-1] = false;
                    }
                    return true;
                }
            }
            return false;
            //TODO: make use of return for solving boards later
        }
        public boolean undo(){
            //TODO: provide practically large array
            if(last2 != last) {
                int temp = last2;
                last2 = value;
                value = last;
                last = temp;
                return true;
            }
            return false;
        }
        public boolean redo(){
            if(last!=value){
                int temp = value;
                value = last2;
                last2 = last;
                last = temp;
                return true;
            }
            return false;
        }

    }
class layout{
        float rating;
        Vector<region> regions;
        int rCount;
        layout(){
            regions = new Vector<>();
        }
        public boolean load(){
            //sample layout
            int gID[] = new int[]{1, 2, 3, 3, 4, 5, 6, 7, 8, 9};
            int typeID[] = new int[]{0, 1, 1, 0, 1, 1, 1, 1, 1, 1};
            int colorSet[] = new int[]{
                    Color.rgb(255,192,0),
                    Color.rgb(146,208,80),
                    Color.rgb(75,172,198),
                    Color.rgb(191,191,191),
                    Color.rgb(217,151,149),
                    Color.rgb(255,255,255),
                    Color.rgb(247,150,70),
                    Color.rgb(182,221,232),
                    Color.rgb(178,161,199),
                    Color.rgb(186,178,132)
            };
            int innerSet[] =  new int[]{0, 46, 5689, 5689, 689, 169, 2356, 89, 123, 47};
            int foreignCount[] =  new int[]{1, 0, 1, 1, 1, 2, 2, 2, 2, 0};
            int foreignSet[] =  new int[]{42, 62, 62356, 15, 64, 28, 695, 38, 894, 18, 897, 479};
            rCount = 10;
            Vector<Integer> ftemp;

            int j=0, k;
            for(int i=0; i<rCount; i++) {
                ftemp = new Vector<>();
                for(k=0;k<foreignCount[i];k++) {
                    ftemp.add(foreignSet[j+k]);
                }
                j+=k;
                region.inner_type type;
                if (typeID[i] == 0) type = region.inner_type.ADDED;
                else type = region.inner_type.REMOVED;
                region r = new region();
                r.init(gID[i], innerSet[i], type, ftemp);
                r.color = colorSet[i];
                regions.add(r);
            }
            return true;
        }
    }

class level {
        public int theme;
        public Context parent;
        cell board[][];
        public layout l;
        public Paint p;
        private int total_count[];
        Vector<Integer> sum;

        private float inner_grid_thickness;
        private float outer_grid_thickness;
        public int cell_size;
        private int small_font_width;
        private int small_font_height;
        private int font_width;
        private int font_height;
        private Point board_offset;

        private Point MAX;

        private int cur_group;
        private int cur_cell;
        private Point cur_cell_xy;
        private int cur_num;

        private boolean request_guides = false;
        private boolean request_hints = false;
        private boolean has_focus = false;
        private boolean sum_error = false;
        private boolean copy_error = false;

        //not 9rows X 9columns, instead 9groups X 9cells each
        level(int layoutID, int themeID, Context context) {
            parent = context;
            theme = themeID;
            total_count = new int[9];

            //init drawing tools
            p = new Paint();

            //init and generate board
            int i,j;
            board = new cell[9][9];
            for(i=0;i<9;i++)
                for(j=0;j<9;j++)
                    board[i][j]=new cell();
            //TODO uncomment generate();

            //load layout: preset, randomly or by difficulty
            l = new layout();       // l.load(/*layout_ID*/);
            //TODO uncomment l.load();

            //calculate sums by region
            //TODO uncomment sum = new Vector<>();
            //TODO uncomment for (i=0; i<l.rCount; i++)
            //TODO uncomment sum.add(findSum(l.regions.get(i)));

            //verify by sum
            //TODO uncomment int total = 405;
            //TODO uncomment for (i=0;i<l.rCount;i++)
            //TODO uncomment total-=sum.get(i);
            //TODO uncomment if(total != 0) sum_error = true;
        }
        public void initMetrics(int sx, int sy){
            //store screen metrics
            MAX = new Point(sx,sy);

            //calculate board dimensions
            inner_grid_thickness = sx/220;
            outer_grid_thickness = sx/120;
            board_offset = new Point(0,0);
            cell_size = (int)(11.2*sx/100);
            font_width = cell_size-20;
            font_height = cell_size-17;
            small_font_width = cell_size/3-20;
            small_font_height = cell_size/3-10;
        }
        public void update(Point current_cell){
            int x,y;
            cur_cell_xy = new Point(current_cell);
            x=current_cell.x/3;
            y=current_cell.y/3;
            cur_group = y*3+x;
            x=current_cell.x%3;
            y=current_cell.y%3;
            cur_cell = y*3+x;
            cur_num = board[cur_group][cur_cell].value;
        }
        private int random(int min, int max){
            return new Random().nextInt((max - min) + 1) + min;
        }
        private void generate(){
            //randomly fill right-diagonal groups
            randomFill(0);
            randomFill(4);
            randomFill(8);

            //fill remaining groups
            logicFill(1,0);

            swapLines(random(9,10));
        }

        private void randomFill(int gID){
            int mask = 0,i,num;
            for (i = 0; i<9; i++) {
                do {
                    num = random(1, 9);
                }while ((mask & (1<<num)) != 0);		//TIL: bool interferes with bitwise neighbours
                mask |= 1 << num;
                fillCell(gID, i, num);
            }
        }
        boolean logicFill(int gID, int cID) {
            if (cID > 8) {
                if (gID == 3) gID += 1;
                if (gID == 7) return true;
                return logicFill(gID + 1, 0);
            }
            for (int i = 1; i <= 9; i++)
            {
                if (isSafe(gID,cID,i))				//if value is possible
                {
                    board[gID][cID].value = i;
                    if (logicFill(gID, cID + 1)) return true;
                    board[gID][cID].value = 0;
                }
            }
            return false;
        }
        private boolean isSafe(int groupID, int cellID, int val) {
            int i;
            Point cellPt = convert(groupID, cellID);				//find row & column of this cell
            for (i = 0; i < 9; i++) {
                if (board[groupID][i].value == val) return false;
                if (i != cellPt.y) {								//horizontal
                    Point temp = convert(cellPt.x, i);		        //here x=group_ID, y=cell_ID, after conversion
                    if (board[temp.x][temp.y].value == val)			//if this cell has same value
                        return false;									//then return not-safe
                }
                if (i != cellPt.x) {								//vertical
                    Point temp = convert(i, cellPt.y);
                    if (board[temp.x][temp.y].value == val)
                        return false;
                }
            }
            return true;
        }
        private void fillCell(int groupID, int cellID, int val){
            int i;

            //fill this cell
            if(board[groupID][cellID].fill(val) && val!=0) {                      //fill(), by default, sets other values to false
                for (i = 0; i < 9; i++)                                 //eliminate copies in sibling cells
                    if(i!=cellID)
                        for (int j = 0; j < 9; j++) {                   //if incoming value is zero, no siblings are cleared
                            if (j+1 == val) board[groupID][i].values[j] = false;
                        }
                //eliminate collinear copies
                Point cellPt = convert(groupID, cellID);                //find row & column of this cell

                for(i=0; i<9; i++){                                     //horizontal
                    if(i!=cellPt.y){
                        Point temp = convert(cellPt.x, i);              //here x=group_ID, y=cell_ID, after conversion
                        if(board[temp.x][temp.y].value==0)              //if this cell is empty
                            board[temp.x][temp.y].values[val-1] = false;  //then set possibility of val to false
                    }
                }
                for(i=0; i<9; i++){                                     //vertical
                    if(i!=cellPt.x){
                        Point temp = convert(cellPt.y, i);
                        if(board[temp.x][temp.y].value==0)
                            board[temp.x][temp.y].values[val-1] = false;
                    }
                }
            }
        }

        @SuppressWarnings("WeakerAccess")
        public Point convert(int row /*or group_ID*/, int col /*or cell_ID*/){
            //TODO: decide whether to make this less confusing by accepting cell_ID,group_ID and returning col,row in that order
            return new Point((row/3)*3 + col/3, (row%3)*3 + col%3);
        }

        private int findSum(region r){
            int sum=0, i=0;
            //    a region is defined as having something added or removed from a group:
            //    int groupID;
            //    Vector<Integer> added;
            //    Vector<Integer> removed;
            //    Vector<Point> foreigner;	    //for foreigners, f.x stores groupID, f.y stores cellID
            if(r.removed==null)                 //if nothing has been removed then something has been added
            {
                if(r.added.size()==0){
                    for(i=0; i<9; i++)
                        sum+=board[r.groupID-1][i].value;
                }
                else if(r.added!=null)
                while (i < r.added.size()) {
                    //addend is board's group's     cell's       value
                    int val = board[r.groupID-1][r.added.get(i)-1].value;
                    sum += val;
                    i++;
                }
            }
            else while(i<9){                    //or if something has been removed, we skip that while adding
                boolean skip = false;
                for(int j=0;j<r.removed.size();j++)
                    if(i==r.removed.get(j)){    //if this cell_ID is in the list of removed cells
                        skip = true;            //we skip adding it
                        break;
                    }
                if(!skip) sum += board[r.groupID-1][i].value;
                i++;
            }
            i=0;
            while(i<r.foreigner.size()){    //foreigners, if any exist, are added
                Point f = r.foreigner.get(i);   //pick one
                sum += board[f.x][f.y].value;   //and add its value
                i++;
            }
            return sum;
        }
        private void drawGrid(Canvas c){
            Paint p = new Paint();
            p.setColor(Color.BLACK);
            for(int i=0;i<20;i++) {
                p.setStrokeWidth(inner_grid_thickness);
                if (i == 0 || i == 9 || i == 10 || i == 19) p.setStrokeWidth(outer_grid_thickness);
                else if(i==3 || i==6 || i==13 || i==16) p.setStrokeWidth(outer_grid_thickness*2/3);
                int x, x2, y, y2;
                if (i < 10) {
                    x = board_offset.x + i * cell_size;
                    x2 = x;
                    y = board_offset.y;
                    y2 = y + 9 * cell_size;
                } else {
                    x = board_offset.x;
                    x2 = x + 9 * cell_size;
                    y = board_offset.y + (i-10) * cell_size;
                    y2 = y;
                }
                c.drawLine(x, y, x2, y2, p);
            }
        }
        private void drawGuides(Canvas c){
            p.setColor(Color.BLACK);
            p.setAlpha(45);
            int x,y;
            //if guidelines are on, rows & cols of similar cells are shaded
            if(request_guides)
                for (int i = 0; i < 9; i++)
                    for (int j = 0; j < 9; j++) {
                        if(!(cur_cell_xy.x == i && cur_cell_xy.y ==j))
                            if (board[i][j].value == cur_num) {
                                x = board_offset.x + i * cell_size;
                                y = board_offset.y + j * cell_size;
                                c.drawRect(board_offset.x, y, x, y + cell_size, p);
                                c.drawRect(x, board_offset.y, x + cell_size, y, p);
                            }
                    }
            //the row, col & group of focused cell is shaded
            x = board_offset.x + cur_cell_xy.x*cell_size;
            y = board_offset.y + cur_cell_xy.y*cell_size;
            c.drawRect(board_offset.x, y, x, y + cell_size, p);
            c.drawRect(x, board_offset.y, x + cell_size, y, p);
            c.drawRect(x-cell_size, y-cell_size, x+cell_size, y+cell_size, p);
            p.setAlpha(255);
        }
        private void shadeRegions(Canvas c){
            for(int id=0;id<l.regions.size();id++){        //loop through all regions
                region r = l.regions.get(id);

                p.setColor(getColor(id));
                int i=0,x,y;
                if(r.removed==null)                     //when added cells are shaded
                {
                    if(r.added.size()==0){
                        for(int j=0; j<9; j++) {
                            Point tmp = convert(r.groupID-1, j);
                            x = tmp.y * cell_size;                //adding cell offset
                            y = tmp.x * cell_size;
                            c.drawRect(x, y,  x + cell_size, y + cell_size, p);
                        }
                    }
                    else if(r.added!=null)
                    while (i<r.added.size())         //here i is used as index to get cell_ID
                    {
                        Point tmp = convert(r.groupID-1, r.added.get(i)-1);
                        x = tmp.y * cell_size;                //adding cell offset
                        y = tmp.x * cell_size;
                        c.drawRect(x, y, x + cell_size, y + cell_size, p);
                        i++;
                    }
                }
                    //when cells excluding removed are shaded
                else while(i<9)                         //here i is itself the cell_ID
                {
                    boolean skip = false;
                    for(int j=0;j<r.removed.size();j++)
                        if(i==r.removed.get(j)) {       //skips the excluded cells
                            skip = true;
                            break;
                        }
                    if(!skip){
                        x= board_offset.x + (r.groupID-1)%3 * 3*cell_size;
                        y= board_offset.y + (r.groupID-1)/3 * 3*cell_size;
                        x+= i%3 *cell_size;
                        y+= i/3 *cell_size;
                        c.drawRect(x,y,x+cell_size,y+cell_size,p);
                    }
                    i++;
                }
                i=0;                                    //when foreign cells are shaded
                while(i<r.foreigner.size())         //here i is index to a Point that stores group_ID, cell_ID
                {
                    Point f = r.foreigner.get(i);       //x stores group ID, y stores cell ID
                    x= board_offset.x + f.x%3 * 3*cell_size;
                    y= board_offset.y + f.x/3 * 3*cell_size;
                    x+=f.y%3 * cell_size;
                    y+=f.y/3 * cell_size;
                    c.drawRect(x,y,x+cell_size,y+cell_size,p);
                    i++;
                }
            }
        }
        private void paintPens(Canvas c){
            for (int i = 0; i < 9; i++)
                for (int j = 0; j < 9; j++) {
                    Point offset = convert(i,j);
                    int x = board_offset.x + offset.y * cell_size + (cell_size - font_width);
                    int y = board_offset.y + (offset.x+1) * cell_size - (cell_size - font_height);
                    p.setColor(Color.GRAY);
                    p.setTextSize(small_font_height);
                    for (int k = 0; board[i][j].value == 0 && k < 9; k++)       //drawing pencil markings
                        if (board[i][j].values[k]) {
                            int dx = (k % 3) * small_font_width;
                            int dy = (k / 3) * small_font_height;
                            c.drawText(Integer.toString(k), x + dx, y + dy, p);
                        }
                    p.setColor(Color.BLACK);
                    p.setTextSize(font_height);                                 //draw
                    c.drawText(Integer.toString(board[i][j].value), x, y, p);
                }
        }

        void swapLines(int times) {
            for (int i = 0; i<times; i++)
            {
                int offset, line1, line2;
                offset = random(0, 2);											//select a group-offset
                line1 = random(0, 2);											//select two lines passing at this offset
                do line2 = random(0, 2); while (line1 == line2);

                _swapLines(offset*3 + line1, offset*3 + line2, random(0, 1) == 0);	//swap all cells between these rows or columns
            }
        }
        void _swapLines(int l1, int l2, boolean rowOrcol) {
            int temp;
            Point pos1, pos2;
            for (int i = 0; i < 9; i++) {
                if (rowOrcol) {
                    pos1 = convert(l1, i);
                    pos2 = convert(l2, i);
                }
                else {
                    pos1 = convert(i, l1);
                    pos2 = convert(i, l2);
                }
                temp = board[pos2.x][pos2.y].value;
                board[pos2.x][pos2.y].value = board[pos1.x][pos1.y].value;
                board[pos1.x][pos1.y].value = temp;
            }
        }
        public void draw(Canvas canvas){
            canvas.drawColor(Color.WHITE);
            //drawGuides();
            //shadeRegions(canvas);
            //drawGrid(canvas);
            //paintPens(canvas);
        }
        private int getColor(int gID){
            int x;
            switch(theme){
                case 0:
                    x=parent.getResources().getIntArray(R.array.feltColor)[gID];
                break;
                case 1:
                    x=parent.getResources().getIntArray(R.array.paperColor)[gID];
                    break;
                case 2:
                    x=parent.getResources().getIntArray(R.array.pastelColor)[gID*3];
                    break;
                default:
                    x=0;
                    break;
            }
            return x;
        }
    }