import edu.sdccd.cisc191.template.Order;
import edu.sdccd.cisc191.template.PastOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Database {

    private PastOrderRepository repository;
    @Autowired
    public Database(PastOrderRepository repository) {
        this.repository = repository;
    }
    // Method to save the order to the database
    public void saveOrder(Order order) {

        try {
            if(repository == null) {
                System.out.println("ERROR: Repository is null, cannot save.");
            }
            repository.save(order); // Save the order to the repository
            System.out.println("Order saved successfully.");
        } catch (Exception e) {
            System.out.println("ERROR: Failed to save order.");
            e.printStackTrace();
        }
    }
}