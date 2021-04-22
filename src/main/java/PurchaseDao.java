import java.sql.*;
import org.apache.commons.dbcp2.*;

public class PurchaseDao {
  private static BasicDataSource dataSource;

  public PurchaseDao() {
    dataSource = DBCPDataSource.getDataSource();
  }

  public void createPurchase(Purchase newPurchase) {
    Connection conn = null;
    PreparedStatement preparedStatement = null;
    String insertQueryStatement = "INSERT INTO Purchases (storeID, custID, date, items) " +
        "VALUES (?,?,?,?)";

    try {
      conn = dataSource.getConnection();
      preparedStatement = conn.prepareStatement(insertQueryStatement);
      preparedStatement.setInt(1, newPurchase.getStoreID());
      preparedStatement.setInt(2, newPurchase.getCustID());
      preparedStatement.setString(3, newPurchase.getDate());
      preparedStatement.setString(4, newPurchase.getItems());

      // execute insert SQL statement
      preparedStatement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }

  }
}