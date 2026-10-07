import type { Expense, IncomeEntry, PaymentReview } from '../context/AppContext';
import { sourceNames } from './paymentModels';
import type { CapturedPayment } from './paymentModels';
type PaymentState = { expenses:Expense[]; incomes:IncomeEntry[]; paymentReviews:PaymentReview[]; capturedIds:string[] };
export function applyCapturedPayments<T extends PaymentState>(previous:T, events:CapturedPayment[]):T {
          let next = previous;
          const seen = new Set(previous.capturedIds);
          for (const event of events) {
            if (!event.id || seen.has(event.id) || !Number.isFinite(event.amount) || event.amount <= 0 || !Number.isFinite(event.timestamp)) continue;
            const date = new Date(event.timestamp);
            const localDate = `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,"0")}-${String(date.getDate()).padStart(2,"0")}`;
            if (!Number.isFinite(date.getTime())) continue;
            seen.add(event.id);
            const source = sourceNames[event.source] || event.source;
            if (event.kind === "expense") {
              next = { ...next, expenses: [{ id: event.id, captureId: event.id, source, date: localDate, time: date.toLocaleTimeString("en-GB", { hour:"2-digit", minute:"2-digit" }), amount: event.amount, category:"Others", paymentType: ["com.phonepe.app", "net.one97.paytm", "com.google.android.apps.nbu.paisa.user", "in.org.npci.upiapp"].includes(event.source) ? "UPI" : "Bank", notes:`Payment notification · ${source}`, upiRef: event.reference || undefined }, ...next.expenses] };
            } else if (event.kind === "income") {
              next = { ...next, incomes: [{ id:event.id, captureId:event.id, source, date:localDate, amount:event.amount, name:"Money received", category:"Uncategorized", notes:`Credit notification · ${source}` }, ...next.incomes] };
            } else {
              next = { ...next, paymentReviews:[{ ...event, status:event.kind === "transfer" ? "transfer" : "review" }, ...next.paymentReviews] };
            }
          }
          return next === previous ? previous : { ...next, capturedIds:[...seen] };
}
