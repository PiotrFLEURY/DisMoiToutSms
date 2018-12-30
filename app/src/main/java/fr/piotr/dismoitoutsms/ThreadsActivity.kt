package fr.piotr.dismoitoutsms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.piotr.dismoitoutsms.messages.Thread
import kotlinx.android.synthetic.main.activity_threads.*
import androidx.core.app.NavUtils



class ThreadsActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ThreadsActivity"
        const val EVENT_OPEN_THREAD = "$TAG.EVENT_OPEN_THREAD"
        const val EXTRA_OPEN_THREAD = "$TAG.EXTRA_OPEN_THREAD"
    }

    class ThreadViewHolder(view: View): RecyclerView.ViewHolder(view) {

        fun setTitle(title: String) {
            itemView.findViewById<TextView>(R.id.tv_thread_title)?.text = title
        }

        fun setText(text: String) {
            itemView.findViewById<TextView>(R.id.tv_thread_body)?.text = text
        }

        fun setDate(date: String) {
            itemView.findViewById<TextView>(R.id.tv_thread_date)?.text = date
        }

        fun setOnClick(thread: Thread) {
            itemView.setOnClickListener{
                val intent = Intent(EVENT_OPEN_THREAD)
                intent.putExtra(EXTRA_OPEN_THREAD, thread)
                LocalBroadcastManager.getInstance(itemView.context).sendBroadcast(intent)
            }
        }

    }

    class ThreadsAdapter(val context: Context, private var threads: List<Thread> = listOf()): RecyclerView.Adapter<ThreadViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
            return ThreadViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.cell_thread, parent, false))
        }

        override fun getItemCount(): Int = threads.size

        override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
            val thread = threads[position]
            holder.setTitle(thread.recipientIds.joinToString(" "))
            holder.setText(thread.snippet)
            holder.setDate(DateUtils.formatDateTime(context, System.currentTimeMillis() - thread.date.time, DateUtils.FORMAT_SHOW_TIME))
            holder.setOnClick(thread)
        }

        fun updateData(datas: List<Thread>) {
            Log.d(TAG, "updatingData with ${datas.size} threads")
            threads = datas
            notifyDataSetChanged()
        }
    }

    private val threadsAdapter: ThreadsAdapter = ThreadsAdapter(this)
    private val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when(intent.action) {
                    EVENT_OPEN_THREAD -> openThread(intent.getSerializableExtra(EXTRA_OPEN_THREAD) as Thread)
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_threads)
        title = getString(R.string.threads_title)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        rv_threads.hasFixedSize()
        rv_threads.layoutManager = LinearLayoutManager(this)
        rv_threads.adapter = threadsAdapter

    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter(EVENT_OPEN_THREAD))
        threadsAdapter.updateData(MyMessagesManager.fetchThreads(this))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    fun openThread(thread: Thread) {
        val intent = Intent(this, ComposeSmsActivity::class.java)
        intent.putExtra(ComposeSmsActivity.EXTRA_THREAD, thread)
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Respond to the action bar's Up/Home button
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
