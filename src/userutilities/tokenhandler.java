package userutilities;

import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.util.Calendar;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.SignatureException;
 
public class tokenhandler {
	String secret = "azulejocanarioperropulpomisscastor";
	@SuppressWarnings("deprecation")
	public String tokenbuilder(String username,String role,int string,int id){
		String jwt=null;
		try {
			jwt = Jwts.builder()
			  .setSubject("users")
			  .setExpiration(oneweek())
			  .claim("name", username)
			  .claim("scope", role)
			  .claim("industria", string)
			  .claim("id",id)
			  .signWith(
			    SignatureAlgorithm.HS256,
			    secret.getBytes("UTF-8")
			     
					  )
			  .compact();
		} catch (InvalidKeyException | UnsupportedEncodingException e) {
			jwt=null;
			e.printStackTrace();
		}
		return jwt;
	}
	
	public String[] tokentester (String jwt) {
		String[] userdata = new String[5];
		
		Jws<Claims> claims;
		try {
			claims = Jwts.parser()
			  .setSigningKey(secret.getBytes("UTF-8"))
			  .parseClaimsJws(jwt);
			String username =  claims.getBody().get("name").toString();
			String scope =  claims.getBody().get("scope").toString();
			String industria =  claims.getBody().get("industria").toString();
			String id= claims.getBody().get("id").toString();
			userdata[0]=scope;
			userdata[1]=username;
			userdata[2]=industria;
			userdata[3]="202";
			userdata[4]=id;
		} catch (SignatureException | ExpiredJwtException | UnsupportedJwtException | MalformedJwtException
				| IllegalArgumentException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			userdata[3]="403";
		}
		return userdata;
	}
	
	
	
	
	public java.util.Date oneweek() {
		final Calendar cal = Calendar.getInstance();
	    cal.add(Calendar.DATE, 7);
	    return cal.getTime();
	}
	
}
