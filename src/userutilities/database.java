package userutilities;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.postgresql.jdbc.TimestampUtils;

import JC.basicmath;
import JC.mm1;
import JC.mms;
import netscape.javascript.JSObject;
public class database {
	public ResultSet rs;
	private PreparedStatement pstmt;
	private Connection con;
	private Statement stmt;
	public database(){
		try {
			Class.forName("org.postgresql.Driver");
			this.con= DriverManager.getConnection("jdbc:postgresql://localhost:5432/quemasterv2","postgres" , "masterkey");
		}
		
		catch(Exception e){ e.getStackTrace(); }
	}
	public JSONObject checklogin (String input) throws SQLException {
		String query="SELECT usersystem.userid, usersystem.password, usersystem.name, usersystem.email, usersystem.username, usersystem.industriaiduser, usersystem.roliduser,rol.rolname FROM public.usersystem INNER JOIN public.rol on usersystem.roliduser=rol.rolid where username=? and password = ?;";
		JSONObject userinf = new JSONObject(input);
		boolean exist=false;
		String username = userinf.getString("username");
		String password = userinf.getString("password");
		this.pstmt = con.prepareStatement(query);
		this.pstmt.setString(1, username);
		this.pstmt.setString(2,password);
		this.rs = pstmt.executeQuery();
		int industri=0;
		int id=0;
		String rol=null;
		if(rs.next()){
			 exist=true;
			 id= rs.getInt("userid");
			 industri = rs.getInt("industriaiduser");
			 rol = rs.getString("rolname");
			 userinf.put("industri", industri)
			 		.put("rol", rol)
			 		.put("id", id);
		}
		con.close();
		userinf.put("exist", exist);
		return userinf;
	}
	public String[] turnsighn (int channelid,int userid,int machineid) {
		Timestamp clockin=getcurrentdate();
		String[] userdata = new String[2];
		userdata[0]="200";
		
		try {
			this.pstmt = con.prepareStatement("INSERT INTO public.turn( channelid, userid, starturn, machineid) VALUES ( ?, ?, ?, ?)");
			this.pstmt.setInt(1, channelid);
			this.pstmt.setInt(2, userid);
			this.pstmt.setTimestamp(3, clockin);
			this.pstmt.setInt(4, machineid);
			this.pstmt.executeUpdate();
			machineupdate(machineid,"ocuppied");
		} catch (SQLException e) {
			userdata[0]="403";
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String query="select * from public.turn where userid=? and machineid=? and channelid=? and starturn=? ORDER BY starturn DESC LIMIT 1;";
		try {
			this.pstmt= con.prepareStatement(query);
			this.pstmt.setInt(1, userid);
			this.pstmt.setInt(2, machineid);
			this.pstmt.setInt(3, channelid);
			this.pstmt.setTimestamp(4, clockin);
			this.rs = pstmt.executeQuery();
			int id=0;
			while(rs.next()) {
			id=	this.rs.getInt("turnid");
			}
			con.close();
			userdata[1]= Integer.toString(id);
		} catch (SQLException e) {
			userdata[0]="404";
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return userdata;
	}
	
	public JSONObject taketurn(String[] userdata,int industry,int channels) throws SQLException {
		int place=getnumberonque(true,channels);
		Double time= expectedtime(channels);
		String results;
		if (time==0) {
			results= "not enough data";
		}else {
			results= Double.toString(time)+" Minutos";
		}
		this.pstmt = con.prepareStatement("INSERT INTO public.que( userid, industriaid, place, estimatedtime, tiempoentrada, tiemposalida, status , channelid)VALUES ( ?, ?, ?, ?, ?,?,?,?)");
		this.pstmt.setLong(1,Integer.parseInt(userdata[4]));
		this.pstmt.setLong(2,industry);
		this.pstmt.setLong(3, place);//place in que
		this.pstmt.setString(4, results );//estimaged
		this.pstmt.setTimestamp(5, getcurrentdate());
		this.pstmt.setTimestamp(6, getcurrentdate());//a cambiar
		this.pstmt.setString(7, "en cola");
		this.pstmt.setLong(8, channels);
		this.pstmt.executeUpdate();
		int tplace = getnumberonque(false,channels);
		JSONObject turntaken = new JSONObject();
		System.out.println("columplace="+place);
		System.out.println("total="+tplace);
		turntaken.put("Columplace",place);
		turntaken.put("total",tplace);
		turntaken.put("EstimatedTime",results);
		turntaken.put("status", "en cola");
		con.close();
		return turntaken;
	}
	public int getnumberonque(boolean mode,int industry) throws SQLException {
		
		String query ="SELECT que.queid, que.userid, que.industriaid, que.place, que.estimatedtime, que.tiempoentrada, que.tiemposalida, que.status, channels.channelid, channels.serie FROM public.que inner join public.channels on channels.channelid=que.channelid WHERE tiempoentrada::date = now()::date and status = 'en cola' and que.channelid=? ORDER BY estimatedtime DESC";
		this.pstmt = con.prepareStatement(query,ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		System.out.println("these is industry="+industry);
		this.pstmt.setInt(1, industry);
		this.rs = pstmt.executeQuery();
		int rows=0;
		int serie=0;
		System.out.println("existe ultimo?="+rs.last());
		if (rs.last()) {
			if (mode){
				serie= rs.getInt("serie");
				rows= rs.getInt("place");
				System.out.println("rows[0]="+rows );
				if (rows-(serie*1000)==999) {
					rows= (serie*1000);
					if (serie==0) {
						rows=1;
					}
					System.out.println("rows[i]="+rows);
				}else {
					System.out.println("rows[i+1]="+rows);
				rows++;
				}
				System.out.println("these is the place you take: "+rows);
			}else if(mode==false){
				rows = rs.getRow();
			    // Move to beginning
				System.out.println("these is the number of people in line"+rows);
			    rs.beforeFirst();
			};
		}else {
			if (mode) {
				query="SELECT * from channels where channelid= ?";
				this.pstmt = con.prepareStatement(query);
				this.pstmt.setInt(1, industry);
				System.out.println("channels:"+industry);
				this.rs = pstmt.executeQuery();
				while(rs.next()) {
					serie= rs.getInt("serie");
					System.out.println("serie="+serie);
					rows= 1000*serie;
					if (serie==0) {
						rows=1;
					}
					System.out.println("rows"+rows);
				}
			}
		}
		return rows;
	}
	public Object[] confirmnews(String usename,String email){
		Boolean b=false;
		Boolean c=false;
		Boolean d=false;
		String error1="nothing wrong";
		String query = "SELECT userid, password, name, email, username, industriaiduser, roliduser FROM public.usersystem WHERE username=? OR email=?";
		try {
			this.pstmt = con.prepareStatement(query);
			this.pstmt.setString(1, usename);
			this.pstmt.setString(2,email);
			this.rs = pstmt.executeQuery();
			while (rs.next()) {
				  String usernamedb = rs.getString("username");
				  String emaildb=rs.getString("email");
				  System.out.println(usernamedb+" == "+usename);
				  System.out.println(emaildb+" == "+email);
				  if(usernamedb.equals(usename)) {
					  System.out.println("name has problems");
					  b=true;
					  d=true;
					  error1="Thest username has been taken";
				  }
				  if(emaildb.equals(email)) {
					  System.out.println("email has problems");
					  c=true;
					  d=true;
					  error1="the email has been taken";
				  }
				}
			if (b==true && c==true) {
				System.out.println("both have probleman");
				error1="Usernamen and email already taken";
			}
		} catch (SQLException e) {
			d=true;
			error1="Internal Server Error";
			e.printStackTrace();
		}
		Object[] results= new Object[2];
		System.out.println(d);
		System.out.println(error1);
		results[0]=d;
		results[1]=error1;
		return results;
	}
	public double expectedtime(int channelid){
		String query="select que.timetaken,que.tiempoentrada,channels.chanenumber, channels.channelid from que inner join channels on que.channelid=channels.channelid where channels.channelid=? and status='atendido' AND tiempoentrada::date = now()::date AND tiempoentrada > current_timestamp - interval '1 hour'";
		double time=0;
		try {
			System.out.println("id:"+channelid);
			this.pstmt = con.prepareStatement(query);
			this.pstmt.setInt(1, channelid);
			this.rs = pstmt.executeQuery();
			double timetaken=0;
			int sumetime=0;
			int numberofchannels=0;
			double numberofclients = 0;
			while(rs.next()) {
				System.out.println("timetaken: "+sumetime);
				System.out.println("numberofchannels: "+numberofchannels);
				System.out.println("Number of clients: "+numberofclients);
				sumetime += rs.getInt("timetaken");
				
				numberofchannels=rs.getInt("chanenumber");
				numberofclients++;
			}
			
			int timehour=(sumetime/ (60 * 60 * 1000));
			int timemin=(sumetime / (60 * 1000));
			timetaken = timehour/numberofclients;
			if(timemin>55) {
			if (numberofchannels!=1) {
				mms counter = new mms(numberofclients, 60/(timetaken), numberofchannels);
				time= counter.tiempoensistema();
			}else {
				mm1 counter = new mm1(numberofclients,60/(timetaken/ (60 * 60 * 1000)));
				time=counter.tiempoensistema();
			}
			}else {
				time=0;
			}
			System.out.println("time: "+time);
			System.out.println("Time taken:"+timetaken);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		time=time*60;
		return time;
	}
	
	
	public int getnextclient(boolean b) throws SQLException {
		String query =("select*from que WHERE tiempoentrada::date = now()::date  and status = 'en cola' ORDER BY estimatedtime DESC;");
		pstmt = con.prepareStatement(query);
		pstmt.setMaxRows(1); 
		rs = pstmt.executeQuery();
		int place = 0;
		while (rs.next()) {
			  place = rs.getInt("place");
			  System.out.println(place + "\n");
			}
		con.close();
		return place;
	}
	
	public void dropline(String input) {
		
	}
	
	public String[] currentuser(boolean mode, int[] id) {
		String query=null;
		if (mode) {
			 query =("select que.queid, que.userid, que.industriaid, que.place, que.estimatedtime, que.tiempoentrada, que.tiemposalida, que.status,que.turnid ,usersystem.name, usersystem.username,que.timetaken from que inner join usersystem on usersystem.userid=que.userid  WHERE tiempoentrada::date = now()::date  and status = 'atendiendo' and que.queid=? ORDER BY tiempoentrada DESC LIMIT 1 for update");
		}else{
			 query =("select que.queid, que.userid, que.industriaid, que.place, que.estimatedtime, que.tiempoentrada, que.tiemposalida, que.status,que.turnid , usersystem.name, usersystem.username, que.timetaken from que inner join usersystem on usersystem.userid=que.userid  WHERE tiempoentrada::date = now()::date and que.channelid= ? and( status = 'en cola' or (status='atendiendo' and turnid= ?))  ORDER BY tiempoentrada limit 1 for update");
		}
		String[] userdata = new String[5];
		String usuario=null;
		String nombre=null;
		int place=0;
		Timestamp entrada=null;
		int queid=0;
		try {
			this.pstmt = con.prepareStatement(query,ResultSet.TYPE_SCROLL_SENSITIVE, 
			ResultSet.CONCUR_UPDATABLE);		
			this.pstmt.setInt(1, id[0]);
			if (!mode) {
				this.pstmt.setInt(2, id[1]);	
			}
			this.rs = pstmt.executeQuery();
			while(rs.next()) {
				usuario= rs.getString("username");
				nombre=rs.getString("name");
				place = rs.getInt("place");
				entrada = rs.getTimestamp("tiempoentrada");  
				queid= rs.getInt("queid");
				String status= rs.getString("status");
				rs.updateInt("turnid",id[1]);  
					if(mode) {
					  System.out.println(usuario);
					  System.out.println(place);  
					  if (id[3]==1) {
						 status = "atendido";
					  }else {
						 status = "skipped";
					  }
					  rs.updateString("status", status);
					  rs.updateLong("timetaken", diferenceofday(getcurrentdate().getTime(),entrada.getTime()));
					  rs.updateTimestamp("tiemposalida", getcurrentdate());
					}else if (status.equals("en cola")) {
						
					  rs.updateString("status","atendiendo");
					}
				rs.updateRow();
			}
			userdata[3]="200";
			if (!mode) {
				userdata[0]=Integer.toString(place);
				userdata[1]=usuario;
				userdata[2]=nombre;
				userdata[4]=Integer.toString(queid);
				con.close();
			}
		} catch (SQLException e) {
			userdata[3]="500";
			e.printStackTrace();
		}
		return userdata;
	}
	public JSONArray checkchannels(int id) {
		JSONArray output = new JSONArray();
		String query =("select * from channels where industriaid = ?");
		try {
			this.pstmt = con.prepareStatement(query);
			this.pstmt.setInt(1, id);
			this.rs = pstmt.executeQuery();
			int channelid=0;
			String industrianame=null;
			int industriaid=0;
			while (rs.next()) {
				  JSONObject indinfo = new JSONObject();
				  channelid=rs.getInt("channelid");
				  industrianame=rs.getString("channelname");
				  industriaid=rs.getInt("industriaid");
				  indinfo.put("channelid",channelid)
				  		 .put("name", industrianame)
				  		 .put("id",industriaid);
				  output.put(indinfo);
			}
		} catch (SQLException e) {
			System.out.println("Something went wrong");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
	}
	
	public JSONArray getindustries(){
		JSONArray output = new JSONArray();
		String query =("select*from industria EXCEPT SELECT*from industria where industriaid=5"); 
		int industriaid=0;
		String industrianame=null;
		int channels=0;
		int workingh=0;
		boolean existence = false;
		try {
			pstmt = con.prepareStatement(query);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				  JSONObject indinfo = new JSONObject();
				  industriaid=rs.getInt("industriaid");
				  industrianame=rs.getString("industrianame");
				  channels=rs.getInt("channels");
				  workingh=rs.getInt("workinghours");
				  existence=rs.getBoolean("specifycolum");
				  con.close();
					indinfo.put("id", industriaid)
						   .put("name", industrianame)
						   .put("channels", channels)
						   .put("working", workingh)
						   .put("existence", existence);
					output.put(indinfo);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return output;
		
	}
	
	public void register(String username,String pasword,String email, String name ) {
		String query="INSERT INTO public.usersystem(password, name, email, username, industriaiduser, roliduser) VALUES ( ?, ?, ?, ?, ?, ?);";
	    try {
			PreparedStatement preparedStmt = con.prepareStatement(query);
		    preparedStmt.setString (1, pasword);
		    preparedStmt.setString (2, name);
		    preparedStmt.setString (3, email);
		    preparedStmt.setString (4, username);
		    preparedStmt.setInt (5, 5);
		    preparedStmt.setInt (6, 1);
		    preparedStmt.execute();
		    con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Timestamp  getcurrentdate() {
		java.util.Date date = new java.util.Date();
		Timestamp timestamp = new Timestamp(date.getTime());
		return timestamp; 
	}
	private Date yesterday() {
	    final Calendar cal = Calendar.getInstance();
	    cal.add(Calendar.DATE, -1);
	    return cal.getTime();
	}
	public JSONObject checkturn(int id) {
		JSONObject output = new JSONObject();
		String query = "select * from public.turn inner join channels on turn.channelid=channels.channelid where finish IS NULL and userid = ?";
		int channels= 0;
		int turn = 0;
		String name=null;
		try {
			this.pstmt = con.prepareStatement(query);
			this.pstmt.setInt(1, id);
			this.rs = pstmt.executeQuery();
			if(rs.next()) {
				channels=rs.getInt("channelid");
				turn= rs.getInt("turnid");
				name= rs.getString("channelname");
				output.put("channel", channels)
					  .put("turn", turn)
					  .put("code", 200)
					  .put("name",name);
			}else {
				output.put("code",404);
			}
		} catch (SQLException e) {
		output.put("code",500);
		output.put("message","error interno del servidor");
		}
		return output;
	}
	public JSONObject endturn (int turnid) {
		String query = "select * from public.turn WHERE turnid=? for update";
		JSONObject output = new JSONObject();
		try {
			this.pstmt = con.prepareStatement(query,ResultSet.TYPE_SCROLL_SENSITIVE, 
					ResultSet.CONCUR_UPDATABLE);
            pstmt.setInt(1, turnid);
            int computerid=0;
            this.rs = pstmt.executeQuery();
            while (rs.next()) {
            	computerid = rs.getInt("machineid");
				rs.updateTimestamp("finish", getcurrentdate());
				rs.updateRow();
            }
            machineupdate(computerid,"available");
            System.out.println(turnid +" "+getcurrentdate() );
            output.put("code", 200);
		} catch (SQLException e) {
			output.put("code", 500)
				  .put("error","internal server error");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return output;
	}
	public void machineupdate(int id, String state) throws SQLException{
		String query = "UPDATE public.machine SET state=? WHERE machineid=?;";
		
			this.pstmt = con.prepareStatement(query);
            pstmt.setString(1, state);
			pstmt.setInt(2, id );
            pstmt.executeUpdate();
       
	}
	
	
	
	public JSONArray machines(int industry) {
		JSONArray output = new JSONArray();
		String query =("select * from machine where machine.industriaid=? and machine.state='available' order by machineid"); 
		int industriaid=0;
		String industrianame=null;
		try {
			this.pstmt = con.prepareStatement(query);
			System.out.println(industry);
			pstmt.setInt(1, industry);
			this.rs = pstmt.executeQuery();
			while (rs.next()) {
				  JSONObject indinfo = new JSONObject();
				  industriaid=rs.getInt("machineid");
				  industrianame=rs.getString("machinename");
				  con.close();
					indinfo.put("id", industriaid)
						   .put("machine", industrianame);
					output.put(indinfo);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return output;
	}
	
	
	
	private long diferenceofday(long l, long m){
		System.out.println("l: "+l +" m: "+ m);
		  long diff =l-m;
		System.out.println(diff);
		return diff;
	}
	public JSONObject checkforuser(int chn,int ind) {
		String query="select que.queid, que.userid, que.industriaid, que.place,que.timetaken, que.estimatedtime, que.tiempoentrada, que.tiemposalida, que.status,que.turnid from que WHERE tiempoentrada::date = now()::date and que.channelid= ? and que.userid= ? and status = 'en cola'  ORDER BY tiempoentrada limit 1";
		int place=0;
		String time=null;
		int code=0;
		boolean exist=false;
		JSONObject output = new JSONObject();
		try {
			this.pstmt = con.prepareStatement(query);
	    	this.pstmt.setInt(1, chn);
	        this. pstmt.setInt(2, ind);
	        this.rs = pstmt.executeQuery();
	        if (rs.next()) {
	        	exist=true;
	        	place= rs.getInt("place");
	        	time=rs.getString("estimatedtime");
	        	code=200;
	        }
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			code=500;
		}	
		output.put("code", code)
			  .put("Columplace", place)
			  .put("EstimatedTime",time)
			  .put("exist", exist);
		System.out.println(output.toString());
		return output;
		}
	public JSONArray billboard (int empresa) {
		String query= "select que.place,que.turnid,que.channelid,channels.channelname,turn.machineid,machine.machinename from que left join channels on que.channelid=channels.channelid left join turn on que.turnid=turn.turnid left join machine on machine.machineid=turn.machineid where que.industriaid = ? and que.status='atendiendo' and  que.tiempoentrada::date = now()::date";
		JSONArray output = new JSONArray();
		try {
			this.pstmt = con.prepareStatement(query);
			this.pstmt.setInt(1, empresa);
			this.rs = pstmt.executeQuery();
			String channelname=null;
			String computer=null;
			String turn=null;
			while (rs.next()) {
				  JSONObject indinfo = new JSONObject();
				  turn=rs.getString("place");
				  channelname=rs.getString("channelname");
				  computer=rs.getString("machinename");
				  indinfo.put("place",turn)
				  		 .put("channelname", channelname)
				  		 .put("computer",computer);
				  output.put(indinfo);
			}
			con.close();
		} catch (SQLException e) {
			System.out.println("Something went wrong");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
	}
	public int dropline( int ind, int chn,String status) {
		String query= "select que.queid, que.userid, que.industriaid, que.place,que.timetaken, que.estimatedtime, que.tiempoentrada, que.tiemposalida, que.status,que.turnid from que WHERE tiempoentrada::date = now()::date and que.channelid= ? and que.userid= ? and status = 'en cola'  ORDER BY tiempoentrada limit 1 for update";
		int code= 0;
		try {
			this.pstmt = con.prepareStatement(query,ResultSet.TYPE_SCROLL_SENSITIVE, 
					ResultSet.CONCUR_UPDATABLE);		
        	this.pstmt.setInt(1, chn);
            this. pstmt.setInt(2, ind);
            System.out.println(ind);
            System.out.println(chn);
            this.rs = pstmt.executeQuery();
            Timestamp entrada= null;
			System.out.println(status);
			System.out.println("select que.queid, que.userid, que.industriaid, que.place,que.timetaken, que.estimatedtime, que.tiempoentrada, que.tiemposalida, que.status,que.turnid from que WHERE tiempoentrada::date = now()::date and que.channelid= "+chn+" and que.userid= "+ind+" and status = 'en cola'  ORDER BY tiempoentrada limit 1 for update");
            while(rs.next()) {
				entrada = rs.getTimestamp("tiempoentrada");
				rs.updateString("status", status);
				rs.updateLong("timetaken", diferenceofday(getcurrentdate().getTime(),entrada.getTime()));
				rs.updateTimestamp("tiemposalida", getcurrentdate());
				rs.updateRow();
			}
            con.close();
			code= 200;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			code= 500;
		}
            
		return code;
	}
	//SELECT que.queid, que.userid, que.industriaid, que.place, que.estimatedtime, que.tiempoentrada, que.tiemposalida, que.status, que.turnid, que.channelid, que.timetaken , turn.userid ,usersystem.name,channels.channelname FROM public.que left join turn on turn.turnid=que.turnid left join usersystem on turn.userid=usersystem.userid left join channels on channels.channelid=que.channelid where timetaken=  (select max(timetaken) from que)
	public JSONObject reportes(int id, Timestamp time1,Timestamp time2){
		String query="SELECT industria.channels,industria.workinghours ,que.queid, que.userid, que.industriaid, que.place, que.estimatedtime, que.tiempoentrada, que.tiemposalida, que.status, que.turnid, que.channelid, que.timetaken , turn.userid ,usersystem.name,channels.channelname FROM public.que left join industria on industria.industriaid=que.industriaid left join turn on turn.turnid=que.turnid left join usersystem on turn.userid=usersystem.userid left join channels on channels.channelid=que.channelid where tiempoentrada BETWEEN  ? AND ? and que.industriaid=?";
		JSONObject reports = new JSONObject();
		try {
			System.out.println("SELECT industria.channels,industria.workinghours ,que.queid, que.userid, que.industriaid, que.place, que.estimatedtime, que.tiempoentrada, que.tiemposalida, que.status, que.turnid, que.channelid, que.timetaken , turn.userid ,usersystem.name,channels.channelname FROM public.que left join industria on industria.industriaid=que.industriaid left join turn on turn.turnid=que.turnid left join usersystem on turn.userid=usersystem.userid left join channels on channels.channelid=que.channelid where tiempoentrada BETWEEN  "+time1+" AND "+time2+" and que.industriaid="+id);
			this.pstmt = con.prepareStatement(query);
			this.pstmt.setTimestamp(1, time1);
			this.pstmt.setTimestamp(2, time2);
			this.pstmt.setInt(3, id);
			this.rs = pstmt.executeQuery();
			int max=0;//no
			int timetakent=0;
			int timetaken=0;
			int mim=999999999;
			int workinghours=0;//yes
			int chanelnumber=1;//yes
			String fastname=null;
			String fastbox=null;
			String slowname=null;
			String slowbox=null;
			int total=0;//yes
			String status=null;//yes
			System.out.println("time 1 ="+time1.getTime()+"Time 2 ="+time2.getTime());
			long timetdiv=diferenceofday( time2.getTime(),time1.getTime());//no
			System.out.println(timetdiv);
			double days= TimeUnit.DAYS.convert(timetdiv, TimeUnit.MILLISECONDS);
			int aband=0;//yes
			int taken=0;//yes
			int notime=0;//yes
			int skipped=0;//yes
			int fasttime=0;
			int slowtime=0;
			while(rs.next()) {
				status=rs.getString("status");
				chanelnumber=rs.getInt("channels");
				workinghours=rs.getInt("workinghours");
				System.out.println(status);
				if (status.equals("sin tiempo")) {
					notime++;
				}else if(status.equals("abandono")) {
					aband++;
				}else if(status.equals("skipped")) {
					skipped++;
				}else if(status.equals("atendido")) {
					timetaken=rs.getInt("timetaken");
					timetakent+=timetaken;
					if (timetaken>max) {
						max=timetaken;
						slowname=rs.getString("name");
						slowbox=rs.getString("channelname");
						slowtime=timetaken;
					}
					if (timetaken<mim) {
						fastname=rs.getString("name");
						fastbox=rs.getString("channelname");
						fasttime=timetaken;
					}
					taken++;
				};
				total++;
			}
			System.out.println(days+" "+chanelnumber);
			if (days==0) {
				days=1;
			}
			days=taken/(days*chanelnumber);
			double hours=60/basicmath.redondear(timetakent/60000);
			System.out.println(hours);
			System.out.println("60/("+timetakent+"*60000)");
			double time;
			double clientes;
			double encola;
			if (taken>10) {
				if (chanelnumber!=1) {
					System.out.println("days="+days+"hours"+hours+"channelnumer"+chanelnumber);
					mms counter = new mms(days, hours, chanelnumber);
					time= counter.tiempoensistema();
					time=counter.tiempoensistema();
					encola=counter.tiempoencola();
					clientes=counter.clientesenlinea();
				}else {
					mm1 counter = new mm1(days,hours);
					time=counter.tiempoensistema();
					encola=counter.tiempoencola();
					clientes=counter.clientesenlinea();
				}
				reports.put("tiemposistema", time).put("tiempoc",encola).put("clientess",clientes).put("code", 200);
			}else if(taken>0 && taken<10) {
				reports.put("code",206);
			}else {
				reports.put("message", "Sin informacion");
				reports.put("code", 204);
			}
			reports.put("fastest", fastname).put("fastbox", fastbox).put("slowest",slowname).put("slowbox",slowbox ).put("abandonado",aband)
			.put("atendido", taken).put("sintiempo", notime).put("skipped", skipped).put("fastime", fasttime).put("slowtime", slowtime).put("total", total);
		} catch (SQLException e) {
			reports.put("code", 500);
			reports.put("message", "Error interno del servidor");
			e.printStackTrace();
		}
		
		return reports;
	
	}
	
	//SELECT que.queid, que.userid, que.industriaid, que.place, que.estimatedtime, que.tiempoentrada, que.tiemposalida, que.status, que.turnid, que.channelid, que.timetaken , turn.userid ,usersystem.name,channels.channelname FROM public.que left join turn on turn.turnid=que.turnid left join usersystem on turn.userid=usersystem.userid left join channels on channels.channelid=que.channelid where tiempoentrada BETWEEN  ? AND ? and que.industriaid=?


}
