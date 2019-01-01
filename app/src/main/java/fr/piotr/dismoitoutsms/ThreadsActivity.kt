package fr.piotr.dismoitoutsms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.NavUtils
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.piotr.dismoitoutsms.messages.Thread
import fr.piotr.dismoitoutsms.util.AbstractActivity
import kotlinx.android.synthetic.main.activity_threads.*


class ThreadsActivity : AbstractActivity() {

    companion object {
        const val TAG = "ThreadsActivity"
        const val EVENT_OPEN_THREAD = "$TAG.EVENT_OPEN_THREAD"
        const val EXTRA_OPEN_THREAD = "$TAG.EXTRA_OPEN_THREAD"
        const val EVENT_THREADS_UPDATED = "$TAG.EVENT_THREADS_UPDATED"
        const val EXTRA_THREADS_UPDATED = "$TAG.EXTRA_THREADS_UPDATED"
        const val EVENT_THREAD_COMPLETED = "$TAG.EVENT_THREAD_COMPLETED"
        const val EXTRA_THREAD_COMPLETED = "$TAG.EXTRA_THREAD_COMPLETED"
    }

    class LoadThreadsAsyncTask(private val localBroadcastManager: LocalBroadcastManager, private val limit: Boolean): AsyncTask<Context, Void, List<Thread>>() {

        override fun doInBackground(vararg args: Context): List<Thread> = MyMessagesManager.fetchThreads(args[0], limit)

        override fun onPostExecute(result: List<Thread>) {
            super.onPostExecute(result)
            val intent = Intent(EVENT_THREADS_UPDATED)
            intent.putExtra(EXTRA_THREADS_UPDATED, result as ArrayList)
            localBroadcastManager.sendBroadcast(intent)
        }

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

    class ThreadsAdapter(val context: Context, var threads: List<Thread> = listOf()): RecyclerView.Adapter<ThreadViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
            return ThreadViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.cell_thread, parent, false))
        }

        override fun getItemCount(): Int = threads.size

        override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
            val thread = threads[position]
            holder.setTitle(thread.recipients.joinToString(" "))
            holder.setText(thread.snippet)
            holder.setDate(DateUtils.getRelativeTimeSpanString(thread.date.time, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString())
            holder.setOnClick(thread)
        }

        fun updateData(datas: List<Thread>) {
            Log.d(TAG, "updatingData with ${datas.size} threads")
            threads = datas
            notifyDataSetChanged()
        }

    }

    private val linearLayoutManager = LinearLayoutManager(this)
    private val scollListener = object : RecyclerView.OnScrollListener(){

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                tv_diction.visibility = View.VISIBLE
            } else {
                tv_diction.visibility = View.GONE
            }
        }
    }
    private val threadsAdapter: ThreadsAdapter = ThreadsAdapter(this)
    private val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when(intent.action) {
                    EVENT_OPEN_THREAD -> openThread(intent.getSerializableExtra(EXTRA_OPEN_THREAD) as Thread)
                    EVENT_THREADS_UPDATED -> onThreadsUpdated(intent.getSerializableExtra(EXTRA_THREADS_UPDATED) as ArrayList<Thread>)
                    EVENT_THREAD_COMPLETED -> onThreadCompleted(intent.getSerializableExtra(EXTRA_THREAD_COMPLETED) as Thread)
                }
            }
        }

    }

    private var limit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_threads)
        title = getString(R.string.threads_title)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        rv_threads.hasFixedSize()

        rv_threads.layoutManager = linearLayoutManager
        rv_threads.adapter = threadsAdapter

        if (checkPermissions(AbstractActivity.PERMISSIONS_REQUEST_RESUME,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.RECEIVE_MMS,
                        Manifest.permission.RECEIVE_WAP_PUSH)) {
            val localBroadcastManager = LocalBroadcastManager.getInstance(this)
            loadThreads(localBroadcastManager)
        }

    }

    fun onThreadsScrolled() {
        val firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition()
        val lastVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition()
        for(position in (firstVisibleItemPosition..lastVisibleItemPosition)) {
            val thread = threadsAdapter.threads[position]
            if(thread.messageCount == 0 && thread.complete.get().not()) {
            }
        }
    }

    fun onThreadCompleted(thread: Thread) {
        thread.complete.set(true)
        threadsAdapter.notifyItemChanged(threadsAdapter.threads.indexOf(thread))
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction(EVENT_OPEN_THREAD)
        intentFilter.addAction(EVENT_THREADS_UPDATED)
        intentFilter.addAction(EVENT_THREAD_COMPLETED)
        val localBroadcastManager = LocalBroadcastManager.getInstance(this)
        localBroadcastManager.registerReceiver(receiver, intentFilter)

        rv_threads.addOnScrollListener(scollListener)

        iv_diction.setOnClickListener { toggleTvDiction() }
        tv_diction.setOnClickListener { openDisMoiToutSmsActivity()}
    }

    private fun toggleTvDiction() {
        if(tv_diction.visibility == View.VISIBLE) {
            tv_diction.visibility = View.GONE
        } else {
            tv_diction.visibility = View.VISIBLE
        }
    }

    private fun openDisMoiToutSmsActivity() {
        startActivity(Intent(this, DisMoiToutSmsActivity::class.java))
    }

    private fun loadThreads(localBroadcastManager: LocalBroadcastManager) {
        progress_threads_loading.visibility = View.VISIBLE
        LoadThreadsAsyncTask(localBroadcastManager, limit).execute(this)
    }

    private fun onThreadsUpdated(threads: List<Thread>) {
        progress_threads_loading.visibility = View.GONE
        threadsAdapter.updateData(threads)
        if(limit) {
            limit = false
            loadThreads(LocalBroadcastManager.getInstance(this))
        }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        rv_threads.removeOnScrollListener(scollListener)
        iv_diction.setOnClickListener {  }
        tv_diction.setOnClickListener {  }
    }

    fun openThread(thread: Thread) {
        val intent = Intent(this, ComposeSmsActivity::class.java)
        intent.putExtra(ComposeSmsActivity.EXTRA_THREAD_ID, thread.id)
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
