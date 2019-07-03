package restfullservice;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.tools.ws.wsdl.document.Output;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import userutilities.database;
import userutilities.tokenhandler;

@Path("/rest")
public class restfull {
  public static final Lock webLock = new ReentrantLock();
  tokenhandler token = new tokenhandler();
  String acces = "http://localhost:4200";
  String acces2= "http://localhost:8100";
  String accese ="http://192.168.43.55:4200";
  String accese2 = "http://192.168.43.55:8100";
  // This method is called if TEXT_PLAIN is request
  //modificado
  @POST
  @Path("/taketurn")
  @Produces(MediaType.APPLICATION_JSON)
  public Response takeaturn(String input,@Context HttpServletRequest request) throws SQLException {
	  webLock.lock();
	  String url=geturl(request);
	  boolean allow=requestacces(url);
	  database location = new database();
	  JSONObject output =new JSONObject(input);
	  try {
		  String jwt = output.getString("token");
		  int ind = output.getInt("id");
		  int chn = output.getInt("idc");
		  String[] userdata = token.tokentester(jwt);
		  userdata[1]=output.getString("accion");
		  output = location.taketurn(userdata,ind,chn);
	  }finally {
		  webLock.unlock();
	  }
	  String outputs = output.toString();
	  if(allow) {
		System.out.println(outputs);
	  	return Response.status(200).entity(outputs).header("Access-Control-Allow-Origin", url).build();
	  }else {
		  return Response.status(200).entity(outputs).header("Access-Control-Allow-Origin", acces).build(); 
	  }
  }
  
  
  @POST
  @Path("/dropturn")
  @Produces(MediaType.APPLICATION_JSON)
  public Response dropturn(String input,@Context HttpServletRequest request)  {
	  String url=geturl(request);
	  boolean allow=requestacces(url);
	  database location = new database();
	  JSONObject output =new JSONObject(input);
	  String jwt = output.getString("token");
	  int chn = output.getInt("idc");
	  String status= output.getString("accion");
	  
	  if (status.equals("yes")) {
		  status="sin tiempo";
	  }else {
		  status="abandono";
	  }
	  String[] userdata = token.tokentester(jwt);
	  int ind = Integer.parseInt(userdata[4]);
	  output = output.put("code", location.dropline(ind,chn,status));
	  String outputs = output.toString();
	  System.out.println(outputs);
	  if(allow) {
		  return Response.status(200).entity(outputs).header("Access-Control-Allow-Origin", url).build();
	  }
	  else {
		  return Response.status(200).entity(outputs).header("Access-Control-Allow-Origin", acces).build();
	  }
	  }
  
  @GET
  @Path("/tokentest")
  @Produces(MediaType.APPLICATION_JSON)
  public Response tokentest( @QueryParam("token")String input,@Context HttpServletRequest request ){
	  String url=geturl(request);
	  boolean allow=requestacces(url);
	  JSONObject output =new JSONObject();
	  output.put("connection", "succes");
	  String[] userdata = token.tokentester(input);
	  if (userdata[3]=="202") {
		  output.put("status", 202);
		  output.put("rol",userdata[0]);
		  output.put("industria",userdata[2]);
		  output.put("username",userdata[1]);
	  }else {
		  output.put("status", 403);
	  }
	  System.out.println(output.toString());
	  if (allow) {
		  return Response.status(200).header("Access-Control-Allow-Origin", url).entity(output.toString()).build();
	  }
	  return Response.status(200).header("Access-Control-Allow-Origin", acces).build();
	}
  @GET
  @Path("/billboard")
  @Produces(MediaType.APPLICATION_JSON)
  public Response billboard( @QueryParam("token")String input,@Context HttpServletResponse response ){
	  response.addHeader("Access-Control-Allow-Origin", acces);
	  System.out.println(input);
	  JSONArray output =new JSONArray();
	  database location = new database();
	  String [] userinfo= token.tokentester(input);
	  System.out.println(userinfo[2]+" "+userinfo[1]+ " "+ userinfo[4] +" "+userinfo[3]);
	  int id =  Integer.parseInt(userinfo[2]);
	  output=location.billboard(id);
	  System.out.println(output.toString());
	  return Response.status(200).header("Access-Control-Allow-Origin", acces).entity(output.toString()).build();
  }
  @POST
  @Path("/login")
  @Produces(MediaType.APPLICATION_JSON)
  public Response loginuser(String input,@Context HttpServletRequest request) throws SQLException {
	  JSONObject output =new JSONObject();
	  database location = new database();
	  String url=geturl(request);
	  System.out.println(url);
	  boolean allow=requestacces(url);
	  String tokenn = null;
		  output=location.checklogin(input);
		  if(output.getBoolean("exist")) {
			   tokenn = token.tokenbuilder(output.getString("username"), output.getString("rol"), output.getInt("industri"),output.getInt("id"));
			  output.put("token",tokenn);
			  output.remove("password");
			  output.put("response", "login sucesfull").put("status", "202");
		  }else {
			  output.remove("password");
			  output.remove("username");
			  output.put("response", "Wrong username or password").put("status", "403");
		  }
	  //remove "domain localhost on production".
		  if (allow) {
			  return Response.status(200).entity(output.toString()).header("Access-Control-Allow-Origin", url).header("access-control-allow-credentials", true).build(); 
		  }
		  return Response.status(200).header("Access-Control-Allow-Origin", acces).header("access-control-allow-credentials", true).build();
  }

  @POST
  @Path("/register")
  @Produces(MediaType.APPLICATION_JSON)
  public Response registeruser(String input,@Context HttpServletRequest request) throws SQLException {
	  JSONObject inputj = new JSONObject(input);
	  JSONObject output = new JSONObject();
	  database location = new database();
	  String url=geturl(request);
	  boolean allow=requestacces(url);
	  Object checkerror[]=new Object[2];
	  if(inputj.getString("password")!=inputj.getString("repassword")) {
	  checkerror = location.confirmnews(inputj.getString("username").toLowerCase(), inputj.getString("email").toLowerCase());
	  System.out.println("these is what we get on restfull="+ checkerror[0]);
	  if ((boolean) checkerror[0]) {
		  System.out.println(checkerror[1]);
		  output.put("error", checkerror[1])
		  		.put("code", 403);
	  }else {
		  output.put("code", 202)
		  		.put("msg", "Registered");
		  location.register(inputj.getString("username").toLowerCase(), inputj.getString("password"), inputj.getString("email").toLowerCase(), inputj.getString("name"));
	  }
	  }else {
		  output.put("error", "password must be the same")
	  			.put("code", 403);
		  System.out.println("password wrong");
	  }
	  if (allow) {
		  return Response.status(200).entity(output.toString()).header("Access-Control-Allow-Origin", url).build();
	  }
	  return Response.status(200).header("Access-Control-Allow-Origin", acces).build();
  }
  //modificado
  @GET
  @Path("/seeplaces")
  @Produces(MediaType.APPLICATION_JSON)
  public Response seeplaces(@QueryParam("id")int id,@QueryParam("token")String id2,@Context HttpServletRequest request) throws SQLException {
	int count=0;
	String url=geturl(request);
	boolean allow=requestacces(url);
	JSONObject output =new JSONObject();
	database location2 = new database();
	String[] userdata = new String[5];
	userdata = token.tokentester(id2);
  	int idp =  Integer.parseInt(userdata[4]);
  	output= location2.checkforuser(id, idp);
	database location = new database();
	count=location.getnumberonque(false,id);
	output.put("place", count);
	String outputs = output.toString();
	if(allow) {
		return Response.status(200).entity(outputs).header("Access-Control-Allow-Origin", url).build();
	}else {
		return Response.status(200).header("Access-Control-Allow-Origin", acces).build();	
	}
	}
  @GET
  @Path("/channels")
  @Produces(MediaType.APPLICATION_JSON)
  public Response channels(@QueryParam("id")int id,@Context HttpServletRequest request) throws SQLException {
	  String url=geturl(request);
	  boolean allow=requestacces(url);
	  database location = new database();
	   JSONArray indinfo= location.checkchannels(id);
	   if (allow) {
		   return Response.status(200).entity(indinfo.toString()).header("Access-Control-Allow-Origin", url).build();
	   }else {  
		   return Response.status(200).header("Access-Control-Allow-Origin", acces).build();
	   }
	 }
  //modificado
  @POST
  @Path("/nexturn")
  @Produces(MediaType.APPLICATION_JSON)
  public Response passtonext(String input) throws SQLException {
	  JSONObject output=new JSONObject();
	  int[] ids = new int[4];
		try {
			JSONObject data = new JSONObject(input);
			ids[0]=data.getInt("queid");
			ids[1]=data.getInt("turnid");
			ids[2]=data.getInt("id");
			ids[3]=data.getInt("mode");
			output =turnmanager(ids,true);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(output.toString());
	return Response.status(200).entity(output.toString()).header("Access-Control-Allow-Origin", acces).build();
  }
  @POST
  @Path("/report")
  @Produces(MediaType.APPLICATION_JSON)
  public Response report(String input) throws SQLException {
	  JSONObject output=new JSONObject(input);
	  try {
		    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd yyyy",Locale.US);
		    Date parsedDate = dateFormat.parse(output.getString("date1"));
		    Date pasedDate2 = dateFormat.parse(output.getString("date2"));
		    String jwt = output.getString("token");
			String[] userdata = token.tokentester(jwt);
			int ind = Integer.parseInt(userdata[2]);
		    Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
		    Timestamp timestamp2 = new java.sql.Timestamp(pasedDate2.getTime());
		    database location = new database();
		    output=location.reportes(ind, timestamp, timestamp2);
		  	System.out.println(timestamp);
		  	System.out.println(timestamp2);
		} catch(Exception e) {
			e.printStackTrace();
		}
	return Response.status(200).entity(output.toString()).header("Access-Control-Allow-Origin", acces).build();
  }
  @GET
  @Path("/currenturn")
  @Produces(MediaType.APPLICATION_JSON)
  public Response currenturn(@QueryParam("id")int input,@QueryParam("idc")int channel) {
	JSONObject output=new JSONObject();
	System.out.println(input);
	int[] ids = new int[2];
	try {	
		ids[0]=input;
		ids[1]=channel;
		output =turnmanager(ids,false);
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return Response.status(200).entity(output.toString()).header("Access-Control-Allow-Origin", acces).build();
  }
 


  @GET
  @Path("/chekturn")
  @Produces(MediaType.APPLICATION_JSON)
  public Response checkturn(@QueryParam("token")String input) {
	JSONObject output=new JSONObject();
	String[] userinfo;
    userinfo= token.tokentester(input);
  	int id =  Integer.parseInt(userinfo[4]);
  	database location = new database();
  	output= location.checkturn(id);
  	System.out.println("this is output:"+output.toString());
	return Response.status(200).entity(output.toString()).header("Access-Control-Allow-Origin", acces).build();
  }
  
  
  
  @GET
  @Path("/employchannel")
  @Produces(MediaType.APPLICATION_JSON)
  public Response employchannel(@QueryParam("token")String input) {
	JSONArray output=new JSONArray();
	String[] userinfo;
    userinfo= token.tokentester(input);
  	int id =  Integer.parseInt(userinfo[2]);
  	database location = new database();
  	output= location.checkchannels(id);
	return Response.status(200).entity(output.toString()).header("Access-Control-Allow-Origin", acces).build();
  }
  
  
  
  @GET
  @Path("/industries")
  @Produces(MediaType.APPLICATION_JSON)
  public Response industries(@Context HttpServletRequest request) throws SQLException {
	  String url=geturl(request);
	  boolean allow=requestacces(url);
	 database location = new database();
	 JSONArray indinfo= location.getindustries();
	 if(allow) {return Response.status(200).entity(indinfo.toString()).header("Access-Control-Allow-Origin", url).build();
	 }else {
		return Response.status(200).header("Access-Control-Allow-Origin", acces).build();
	 }
	 }

  @GET
  @Path("/machines")
  @Produces(MediaType.APPLICATION_JSON)
  public Response machines(@QueryParam("token")String input) throws SQLException {
	 database location = new database();
	 String[] userinfo;
	 userinfo= token.tokentester(input);
	 int id =  Integer.parseInt(userinfo[2]);
	 JSONArray indinfo= location.machines(id);
	 return Response.status(200).entity(indinfo.toString()).header("Access-Control-Allow-Origin", acces).build();
	}
  
  @POST
  @Path("/endturn")
  @Produces(MediaType.APPLICATION_JSON)
  public Response endturn(String input) throws SQLException {
	  JSONObject inputjson=new JSONObject(input);
	  database location = new database();
	  JSONObject output=new JSONObject();
	  output=location.endturn(inputjson.getInt("turn"));
	return Response.status(200).entity(output.toString()).header("Access-Control-Allow-Origin", acces).build();
  }
  
  @POST
  @Path("/singturn")
  @Produces(MediaType.APPLICATION_JSON)
  public Response signturnt(String input) throws SQLException {
	  JSONObject inputjson=new JSONObject(input);
	  System.out.println(inputjson);
	  String[] userinfo= token.tokentester(inputjson.getString("token"));
	  int id=Integer.parseInt(userinfo[4]);
	  database location = new database();
	  String[] turninfo = location.turnsighn(inputjson.getInt("chid"), id, inputjson.getInt("cid"));
	  JSONObject output = new JSONObject();
	  output.put("code", Integer.parseInt(turninfo[0]));
	  if (output.getInt("code")==200) {
		  output.put("turnid",Integer.parseInt(turninfo[1]));
	  }else {
		  output.put("error", "Internal server error");
	  }
	return Response.status(200).entity(output.toString()).header("Access-Control-Allow-Origin", acces).build();
  }
  public JSONObject turnmanager(int[] ids,Boolean mode) throws SQLException{
	    String[] count2;
	    int id= ids[0];
		database location = new database();
		int count=0;
		if(mode) {
			count2=location.currentuser(mode, ids);
			if (count2[3]=="200") {
				ids[0]=ids[2];
				count=location.getnumberonque(false,ids[0]);
				count2=location.currentuser(false, ids);
			}
		}else{
			count=location.getnumberonque(false,id);
			count2=location.currentuser(mode, ids);
		}
		System.out.println(count);
		JSONObject output =new JSONObject();
		output.put("code", count2[3]);
		output.put("place", count);
		output.put("user", count2[1]);
		output.put("name", count2[2]);
		output.put("queid", count2[4]);
		output.put("currentplace", count2[0]);
		return output;
  }
  	private String geturl(HttpServletRequest request) {
  		String requestUrl = request.getHeader("origin");
  		return requestUrl;
  	}
   private Boolean requestacces(String requestUrl) {
		  boolean allowacces=false;
		  if (requestUrl.equals(acces)||requestUrl.equals(acces2)||requestUrl.equals(accese2)||requestUrl.equals(accese)) {
			  allowacces=true;
		  }
	return allowacces;
	}
  
}