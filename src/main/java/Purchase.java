public class Purchase {
  private int storeID;
  private int custID;
  private String date;
  private String items;

  public Purchase(int storeID, int custID, String date, String items) {
    this.storeID = storeID;
    this.custID = custID;
    this.date = date;
    this.items = items;
  }

  public int getStoreID() {
    return storeID;
  }

  public String getItems() {
    return items;
  }

  public void setItems(String items) {
    this.items = items;
  }

  public void setStoreID(int storeID) {
    this.storeID = storeID;
  }

  public int getCustID() {
    return custID;
  }

  public void setCustID(int custID) {
    this.custID = custID;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }
}