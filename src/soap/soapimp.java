package soap;
import javax.jws.WebService;

//http://localhost:8080/Sirvientedecolas/soap

@WebService(endpointInterface = "soap.soapinter")
public class soapimp implements soapinter {
		@Override
		public String getsoapAsString() {
			// TODO Auto-generated method stub
			return "amarillo";
		}
}
