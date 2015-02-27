package controllers;

import nl.idgis.publisher.domain.web.Constant;
import play.Logger;
import play.Play;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.F;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import actions.DefaultAuthenticator;
import views.html.constants.form;

@Security.Authenticated (DefaultAuthenticator.class)
public class Constants extends Controller {
	private final static String databaseRef = Play.application().configuration().getString("publisher.database.actorRef");
	private final static String ID="#CREATE_CONSTANTS#";
	
	
	private static Promise<Result> renderCreateForm (final Form<ConstantsForm> constantsForm) {
		// No need to go to the database, because the form contains all information needed
		 return Promise.promise(new F.Function0<Result>() {
             @Override
             public Result apply() throws Throwable {
                 return ok (form.render (constantsForm, true));
             }
         });
	}
	
	public static Promise<Result> create () {
		Logger.debug ("create Constants");
		final Form<ConstantsForm> constantsForm = Form.form (ConstantsForm.class).fill (new ConstantsForm ());
		
		return renderCreateForm (constantsForm);
	}
	
	public static class ConstantsForm {
		
		@Constraints.Required
		private String id;
		private String contact;
		private String organization;
		private String position;
		private String addressType;
		private String address;
		private String city;
		private String state;
		private String zipcode;
		private String country;
		private String telephone;
		private String fax;
		private String email;
		
		public ConstantsForm(){
			super();
			this.id = ID;
		}
		
		public ConstantsForm(Constant constant){
			this.id = constant.id();
			this.contact = constant.contact();
			this.organization = constant.organization();
			this.position = constant.position();
			this.addressType = constant.addressType();
			this.address = constant.address();
			this.city = constant.city();
			this.state = constant.state();
			this.zipcode = constant.zipcode();
			this.country = constant.country();
			this.telephone = constant.telephone();
			this.fax = constant.fax();
			this.email = constant.email();
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
		
		public String getContact() {
			return contact;
		}

		public void setContact(String contact) {
			this.contact = contact;
		}
		
		public String getOrganization() {
			return organization;
		}

		public void setOrganization(String organization) {
			this.organization = organization;
		}
		
		public String getPosition() {
			return position;
		}

		public void setPosition(String position) {
			this.position = position;
		}
		
		public String getAddressType() {
			return addressType;
		}

		public void setAddressType(String addressType) {
			this.addressType = addressType;
		}
		
		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}
		
		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}
		
		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}
		
		public String getZipcode() {
			return zipcode;
		}

		public void setZipcode(String zipcode) {
			this.zipcode = zipcode;
		}
		
		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}
		
		public String getTelephone() {
			return telephone;
		}

		public void setTelephone(String telephone) {
			this.telephone = telephone;
		}
		
		public String getFax() {
			return fax;
		}

		public void setFax(String fax) {
			this.fax = fax;
		}
		
		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		
	}
}