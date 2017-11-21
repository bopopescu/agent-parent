/*
Copyright (c) 2013 eBay, Inc.
This program is licensed under the terms of the eBay Common Development and
Distribution License (CDDL) Version 1.0 (the "License") and any subsequent  version 
thereof released by eBay.  The then-current version of the License can be found 
at http://www.opensource.org/licenses/cddl1.php and in the eBaySDKLicense file that 
is under the eBay SDK ../docs directory.
*/

package com.ebay.sdk.call;


import com.ebay.sdk.*;
import com.ebay.soap.eBLBaseComponents.*;
/**
 * Wrapper class of the SetStore call of eBay SOAP API.
 * <br>
 * <p>Title: SOAP API wrapper library.</p>
 * <p>Description: Contains wrapper classes for eBay SOAP APIs.</p>
 * <p>Copyright: Copyright (c) 2009</p>
 * <p>Company: eBay Inc.</p>
 * <br> <B>Input property:</B> <code>StoreType</code> - Specifies the Store configuration that is being set for the user.
 * 
 * @author Ron Murphy
 * @version 1.0
 */

public class SetStoreCall extends ApiCall
{
  
  private StoreType storeType = null;


  /**
   * Constructor.
   */
  public SetStoreCall() {
  }

  /**
   * Constructor.
   * @param apiContext The ApiContext object to be used to make the call.
   */
  public SetStoreCall(ApiContext apiContext) {
    super(apiContext);
    

  }

  /**
   * Sets the configuration of the eBay store owned by the caller.
   * 
   * <br>
   * @throws ApiException
   * @throws SdkException
   * @throws Exception
   * @return The void object.
   */
  public void setStore()
      throws ApiException, SdkException, Exception
  {
    SetStoreRequestType req;
    req = new SetStoreRequestType();

    if( this.storeType == null )
      throw new SdkException("StoreType property is not set.");

    if (this.storeType != null)
      req.setStore(this.storeType);

    SetStoreResponseType resp = (SetStoreResponseType) execute(req);


  }

  /**
   * Gets the SetStoreRequestType.storeType.
   * @return StoreType
   */
  public StoreType getStoreType()
  {
    return this.storeType;
  }

  /**
   * Sets the SetStoreRequestType.storeType.
   * @param storeType StoreType
   */
  public void setStoreType(StoreType storeType)
  {
    this.storeType = storeType;
  }

}
