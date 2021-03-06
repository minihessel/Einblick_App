package Model;

import Model.DataInsight.DataIntelligence;
import Controller.SQL_manager;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import org.controlsfx.control.PopOver;

/**
 *
 * @author Eskil Hesselroth
 */
public class Table {

    //En tabell må ha antall rader og hvilken tabell det er i alle tabellene(indeksen i en liste av tabeller)
    //ArrayList<Rader> rows = FXCollections.observableArrayList();
    public ArrayList<Kolonne> listofColumns;
    public int tableNumber;
    public int numberofRows;
    ObservableList<List<String>> dataen;
    public SortedList<List<String>> sortedData;
    public FilteredList<List<String>> filteredItems;
    List<TextField> listOfTxtFields;
    public DataIntelligence datainsight = null;
    public final String NAVN;
    public List<String> rowMessages = new ArrayList();
    PopOver popup;
    Label label = new Label();
    List<TableRow> listofrows = new ArrayList();
    Map<List<String>, String> map = new HashMap();

    public Table(String name) {

        label.setTextFill(Color.web("#0076a3"));
        popup = new PopOver();
        popup.autoHideProperty().set(true);
        popup.setContentNode(label);
        popup.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
        listofColumns = new ArrayList<>();
        dataen = FXCollections.observableArrayList();
        NAVN = name;

    }

    /**
     * Laster inn data fra angitt tabell fra angitt db. Bruker har valgt hvilken tabell - vi laster inn kolonner og rader i tableview
     */
    public void loadData(String SQL, Table tbl, int tableNumb) throws SQLException {
        numberofRows = 0;
        tableNumber = tableNumb;

        SQL_manager.getDataFromSQL(SQL);

        for (int i = 1; i <= SQL_manager.rs.getMetaData().getColumnCount(); i++) {
            String kolonneNavn = SQL_manager.rs.getMetaData().getColumnName(i);

            Kolonne kol;
            int type = SQL_manager.rs.getMetaData().getColumnType(i);
            if (type == Types.DOUBLE) {
                kol = new Kolonne(kolonneNavn, i - 1, tbl, false, true);
            } else if (type == Types.INTEGER) {
                kol = new Kolonne(kolonneNavn, i - 1, tbl, true, false);
            } else {
                kol = new Kolonne(kolonneNavn, i - 1, tbl, false, false);
            }
            listofColumns.add(kol);

        }
        List<String> list = new ArrayList();

        int number = SQL_manager.rs.getMetaData().getColumnCount();
        while (SQL_manager.rs.next()) {

            numberofRows++;
            for (int i = 0; i < number; i++) {
                Kolonne k = listofColumns.get(i);
                k.addField(SQL_manager.rs.getString(k.NAVN));

            }

        }

        //her skal tilkoblingen lukkes, kun fjernet mens jeg tester
        //SQL_manager.conn.close();
    }

    public TableView fillTableView(TableView tableView, Table tbl) {
        listOfTxtFields = new ArrayList();

        //Metode for å fylle tableview med kolonner og rader
        //først henter vi ut alle kolonnene og legger til de i tableview
        int counter = 0;
        int r = 0;
        //denne for løkken legger til kolonner dynamisk.
        //Dette må til da vi på forhånd ikke vet hvor mange kolonner det er og ikke har sjans til å lage en modell som forteller det
        for (Kolonne kol : listofColumns) {

            final int j = counter;
            MyTableColumn col = new MyTableColumn(kol.NAVN);
            int counterz = 0;

            if (kol.amIInteger) {
                col.setCellValueFactory(new Callback<MyTableColumn.CellDataFeatures<ObservableList, Number>, ObservableValue<Number>>() {
                    public ObservableValue<Number> call(MyTableColumn.CellDataFeatures<ObservableList, Number> param) {

                        if (param.getValue().get(j) != null) {
                            return new SimpleIntegerProperty(Integer.parseInt(param.getValue().get(j).toString()));
                        } else {
                            return null;
                        }

                    }
                });

            } else if (kol.amIDouble) {
                col.setCellValueFactory(new Callback<MyTableColumn.CellDataFeatures<ObservableList, Number>, ObservableValue<Number>>() {
                    public ObservableValue<Number> call(MyTableColumn.CellDataFeatures<ObservableList, Number> param) {
                        if (param.getValue().get(j) != null) {
                            return new SimpleDoubleProperty(Double.parseDouble(param.getValue().get(j).toString()));
                        } else {
                            return null;
                        }

                    }
                });
                System.out.println("true3");

            } else if (!kol.amIDouble && !kol.amIInteger) {
                col.setCellValueFactory(new Callback<MyTableColumn.CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
                    public ObservableValue<String> call(MyTableColumn.CellDataFeatures<ObservableList, String> param) {
                        if (param.getValue().get(j) != null) {
                            return new SimpleStringProperty(param.getValue().get(j).toString());
                        }
                        return null;
                    }
                });
            }

            /*
             row.setCellFactory(new Callback<TableColumn<ObservableList, String>, TableCell<ObservableList, String>>() {
             @Override
             public TableCell<ObservableList, String> call(TableColumn<ObservableList, String> row) {
             final TableCell<ObservableList, String> cell = new TableCell<ObservableList, String>() {
             @Override
             public void updateItem(String firstName, boolean empty) {
             super.updateItem(firstName, empty);
             if (empty) {
             setText(null);
             } else {
             setText(firstName);
             }
             }
             };
             row.cellFactoryProperty().
             cell.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

             @Override
             public void handle(MouseEvent event) {

             if (event.getClickCount() > 1) {

             label.setText(rowMessages.get(j));
             popup.show(cell);
             }
             }
             });
             return cell;
             }
             });

             }
             */
            col.setSortable(true);

            col.setUserData(counter);
            //For å legge til filtere på tableView dynamisk bruker jeg denne koden. Jeg lager en ny label, en ny tekstboks
            // disse legger jeg til i en vBoks som jeg setter som grafikkElement på hver eneste kolonne i tableviewet.
            TextField txtField = new TextField();

            // vbox.add(lbl, 0, 1);
            //  vbox.add(txtField, 0, 2);
            //vbox.minWidth(lbl.getText().length() + 50);
            listOfTxtFields.add(txtField);

            txtField.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent ke) {
                    if (ke.getCode().equals(KeyCode.ENTER)) {
                        filter();
                    }
                }
            });

            col.setGraphic(txtField);

            txtField.setMinWidth(Region.USE_PREF_SIZE);
            txtField.prefWidthProperty().bind(col.widthProperty().subtract(8));
            tableView.getColumns().add(col);
            counter++;

        }

        //Her sjekker jeg om kolonnen som kommer er en vanlig eller kombinert kolonne. Er den en kombinert kaller vi på CombineColumns()
        // for å kombinere kolonnen.
        for (Kolonne kol : listofColumns) {
            if (kol.amICombined == true) {
                kol.combineColumns();
            }

            dataen.addAll(kol.allFields());

        }
        //ettersom jeg snakker til data vertikalt(fordi jeg snakker om kolonner), men tableView snakker om data i rader(horisontalt)
        //, snur jeg dataen fra vertikalt til horisontalt ved å bruke transpose.
        dataen = transpose(dataen);

        System.out.println(dataen.size());
        // Her legger jeg til filtreringen på tekstfeltene. Det viktige er at dette skjer dynamisk, fordi jeg ikke vet hvor mange tekstfelter jeg har
        //Bruker lambda funksjon som sier at HVIS det finnes rader som har teksten fra alle tekstfeltene, vis dem
        // med andre ord: den sjekker rett og slett :
        // SHOW DATA; WHERE DATA=txtField1,txtField2 osv.
        filteredItems = new FilteredList(dataen, e -> true);

        tableView.setMinHeight(
                832);

        //for å ikke miste muligheten for å sortere data, legger vi det inn i en sorted list
        sortedData = new SortedList<>(filteredItems);

        //å binder det til tableViewen..Da mister vi ikke sorting funksjonalitet.
        sortedData.comparatorProperty()
                .bind(tableView.comparatorProperty());
        //deretter setter vi tableView til å bruke denne nye "sorted data". 
        tableView.setItems(sortedData);

        System.out.println("stø " + rowMessages.size());
        System.out.println("stæ " + numberofRows);

        if (rowMessages.size() == numberofRows) {
            tableView.setRowFactory(tv -> {

                TableRow<ObservableList> row = new TableRow<>();

                row.setOnMouseClicked((MouseEvent event) -> {
                    if (event.getClickCount() == 2 && (!row.isEmpty())) {
                        int originalRowIndex = dataen.indexOf(tableView.getSelectionModel().getSelectedItem());
                        label.setText(rowMessages.get(originalRowIndex));

                        popup.show(row);

                        //    popup.setAnchorX(tableView.getLayoutX());
                        // popup.setY(((TableRow)event.getSource()).getLocalToSceneTransform().getTy());
                    }
                });

                return row;

            });
        }

        //returnerer tableviewn til tableviewn som kalte på denne metoden
        return tableView;

    }

    public void filter() {

        DateTest dateTest = new DateTest();
        filteredItems.setPredicate(li -> {

            for (int i = 0; i < li.size(); i++) {
                if (dateTest.isValidDate(listOfTxtFields.get(i).getText().replace("a", "").replace("b", ""))) {

                    try {
                        dateTest.isValidDate(li.get(i));
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        Date date1 = sdf.parse(li.get(i));
                        Date date2 = sdf.parse(listOfTxtFields.get(i).getText().replace("a", "").replace("b", ""));
                        if (listOfTxtFields.get(i).getText().contains("a")) {
                            if (date1.after(date2)) {
                                return true;

                            }
                        }
                        if (listOfTxtFields.get(i).getText().contains("b")) {
                            if (!date1.before(date2)) {
                                return false;
                            }
                        } else {
                            if (!date1.equals(date2)) {
                                return false;

                            }
                        }
                    } catch (ParseException ex) {
                        Logger.getLogger(Table.class
                                .getName()).log(Level.SEVERE, null, ex);
                    }

                } else if (listOfTxtFields.get(i).getText().contains("<") && checkForInteger.isInteger(li.get(i))) {
                    if (Float.parseFloat(li.get(i)) > Float.parseFloat(listOfTxtFields.get(i).getText().replace("<", ""))) {
                        return false;
                    }
                } else if (listOfTxtFields.get(i).getText().contains(">") && checkForInteger.isInteger(li.get(i))) {
                    if (Float.parseFloat(li.get(i)) < Float.parseFloat(listOfTxtFields.get(i).getText().replace(">", ""))) {
                        return false;
                    }
                } else {
                    if (!li.get(i).toLowerCase().
                            contains(
                                    listOfTxtFields.get(i).getText().toLowerCase()
                            )) {

                        return false;

                    }

                }

            }

            return true;
        });

    }

    public void removeFilters() {
        for (TextField txtField : listOfTxtFields) {
            txtField.setText("");

        }
        filter();

    }

    public void setDataInsight(DataIntelligence datainsight) {
        this.datainsight = datainsight;
    }

    public DataIntelligence getDataInsight() {
        if (datainsight != null) {
            return datainsight;
        } else {
            return null;
        }
    }

    static <T> ObservableList<List<String>> transpose(ObservableList<List<String>> originalData) {
        ObservableList<List<String>> flippedData
                = FXCollections.observableArrayList();
        final int N = originalData.get(0).size();
        for (int i = 0; i < N; i++) {
            ObservableList<String> row = FXCollections.observableArrayList();
            for (List<String> col : originalData) {
                row.add(col.get(i));
            }

            flippedData.add(row);

        }
        return flippedData;
    }

    @Override
    public String toString() {
        return NAVN;

    }

}
