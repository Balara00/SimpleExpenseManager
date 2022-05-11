package lk.ac.mrt.cse.dbs.simpleexpensemanager.data.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.exception.InvalidAccountException;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.model.Account;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.model.ExpenseType;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.model.Transaction;

public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(Context context) {
        super(context, "190572L", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createAccountTable = "CREATE TABLE ACCOUNT_TABLE (ACCOUNT_NO TEXT PRIMARY KEY, BANK_NAME TEXT, ACCOUNT_HOLDER_NAME TEXT, BALANCE REAL)";
        String createTransactionTable = "CREATE TABLE TRANSACTION_TABLE (ID INTEGER PRIMARY KEY AUTOINCREMENT, DATE TEXT, ACCOUNT_NO TEXT, EXPENSE_TYPE TEXT, AMOUNT REAL, FOREIGN KEY (ACCOUNT_NO) REFERENCES ACCOUNT_TABLE (ACCOUNT_NO))";
        sqLiteDatabase.execSQL(createAccountTable);
        sqLiteDatabase.execSQL(createTransactionTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS ACCOUNT_TABLE");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS TRANSACTION_TABLE");
        onCreate(sqLiteDatabase);
    }

    public List<String> getAccountNumbersList() {
        List<String> returnAccNoList = new ArrayList<>();

        String query = "SELECT ACCOUNT_NO FROM ACCOUNT_TABLE";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String accountNo = cursor.getString(0);

                returnAccNoList.add(accountNo);

            } while (cursor.moveToNext());

        } else {

        }
        cursor.close();
        db.close();
        return returnAccNoList;
    }

    public List<Account> getAccountsList() {
        List<Account> returnAccList = new ArrayList<>();

        String query = "SELECT * FROM ACCOUNT_TABLE";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
//        Cursor cursor = db.query("ACCOUNT_TABLE", null, null, null, null, null, null, null);

        while (cursor.moveToNext()) {
            String accountNo = cursor.getString(0);
            String bankName = cursor.getString(1);
            String accountHolderName = cursor.getString(2);
            double balance = cursor.getDouble(3);

            Account account = new Account(accountNo, bankName, accountHolderName, balance);
            returnAccList.add(account);

        }
        cursor.close();
        db.close();
        return returnAccList;
    }

    public Account getAccount(String accountNo) throws InvalidAccountException {
        Account returnAcc;
        String query = "SELECT * FROM ACCOUNT_TABLE WHERE ACCOUNT_NO = " + accountNo;
        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(query, null);
        Cursor cursor = db.query("ACCOUNT_TABLE", null, "ACCOUNT_NO = ?", new String[]{accountNo}, null, null, null);

        if (!cursor.moveToFirst()) {
            String message = "Account " + accountNo + " is invalid";
            throw new InvalidAccountException(message);
        }

        String bankName = cursor.getString(1);
        String accountHolderName = cursor.getString(2);
        double balance = cursor.getDouble(3);

        returnAcc = new Account(accountNo, bankName, accountHolderName, balance);

        cursor.close();
        db.close();

        return returnAcc;
    }

    public void addAccount(Account account) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("ACCOUNT_NO", account.getAccountNo());
        cv.put("BANK_NAME", account.getBankName());
        cv.put("ACCOUNT_HOLDER_NAME", account.getAccountHolderName());
        cv.put("BALANCE", account.getBalance());

        db.insert("ACCOUNT_TABLE", null, cv);

        db.close();
    }

    public void removeAccount(String accountNo) throws InvalidAccountException {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete("ACCOUNT_TABLE", "ACCOUNT_NO =?", new String[]{accountNo});
        db.close();
    }

    public void updateBalance(String accountNo, ExpenseType expenseType, double amount) throws InvalidAccountException {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query("ACCOUNT_TABLE", new String[]{"BALANCE"}, "ACCOUNT_NO = ?", new String[]{accountNo}, null, null, null);

        if (!cursor.moveToFirst()) {
            String message = "Account" + accountNo + "is invalid.";
            throw new InvalidAccountException(message);
        }
        double balance = cursor.getDouble(cursor.getColumnIndexOrThrow("BALANCE"));

        switch (expenseType) {
            case INCOME:
                balance = balance + amount;
                break;
            case EXPENSE:
                balance = balance - amount;
                break;
        }

        cursor.close();

        db = getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("BALANCE", balance);

        db.update("ACCOUNT_TABLE", cv, "ACCOUNT_NO = ?", new String[]{accountNo});
        db.close();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void logTransaction(Date date, String accountNo, ExpenseType expenseType, double amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        System.out.println(date);
        SimpleDateFormat sd = new SimpleDateFormat("dd-MM-yyyy");

        cv.put("DATE", sd.format(date));
        cv.put("ACCOUNT_NO", accountNo);
        cv.put("EXPENSE_TYPE", String.valueOf(expenseType));
        cv.put("AMOUNT", amount);

        db.insert("TRANSACTION_TABLE", null, cv);

        db.close();
    }

    public List<Transaction> getAllTransactionLogs() {
        List<Transaction> returnTransLogList = new ArrayList<>();

        String query = "SELECT * FROM TRANSACTION_TABLE";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
//        Cursor cursor = db.query("TRANSACTION_TABLE", null, null, null, null, null, null);



        while (cursor.moveToNext()) {
            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
            Date date = null;
            try {
//                Log.d("SQLDate", format.parse(cursor.getString(1)).toString());
//                System.out.println( format.parse(cursor.getString(1)).toString());
                date = format.parse(cursor.getString(1));

                String accountNo = cursor.getString(2);
                ExpenseType expenseType = ExpenseType.valueOf(cursor.getString(3));
                double amount = cursor.getDouble(4);

                Transaction transaction = new Transaction(date, accountNo, expenseType, amount);
                returnTransLogList.add(transaction);
            } catch (ParseException e) {
                e.printStackTrace();
            }

        }

        cursor.close();
        db.close();
        return returnTransLogList;
    }

    public List<Transaction> getPaginatedTransactionLogs(int limit) {
        List<Transaction> returnPagiTransLogList = new ArrayList<>();

        List<Transaction> allTransLog = this.getAllTransactionLogs();
        int size = allTransLog.size();

        if (size <= limit) {
            returnPagiTransLogList = allTransLog;
        } else {
            returnPagiTransLogList.subList(size - limit, size);
        }

        return returnPagiTransLogList;
    }
}
