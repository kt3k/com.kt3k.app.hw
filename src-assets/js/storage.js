(function(){
	var APP_LABEL = '__storage__';

    var SEARCH_SAVE_MAX = 25;
    var JOB_SAVE_MAX = 75;

    var storage = {
    	get: function(key, defaultValue) {
    		key = String(key);
    		var value = device.get(key, "");
    		if (value == "") {
    			return defaultValue;
    		}
    		return JSON.parse(value);
    	},

    	set: function(key, value) {
    		key = String(key);
    		device.set(key, JSON.stringify(value));
    	},

    	remove: function(key) {
    		key = String(key);
    		device.remove(key);
    	},

    	has: function(key) {
            key = String(key);
    		return device.has(key);
    	},

    	clear: function() {
    		device.dataClear();
    	},

    	dumpAll: function() {
    		return device.dumpAll();
    	},
    };

    /**
      job model infomation.
      
      job = {
      	number: "",
      	name: "",
      	employmentType: "",
      	place: "",
      	table: tableHtml,
      }

      job data scheme
      job list: ["01010-04514421"];
      job view data: {"01010-04514421": job};
      job data: "job01010-04514421" --> job

     */
    var jobList = {
        getList: function() {
            return storage.get('jobList', []);
        },

        saveList: function(jobList) {
        	storage.set('jobList', jobList);
    	},

    	remove: function() {
    		storage.remove('jobList');
    	}
    };

    var jobViewData = {
    	getViewData: function() {
    		return storage.get('jobViewData', {});
    	},

    	saveViewData: function(viewData) {
    		storage.set('jobViewData', viewData);
    	},

    	remove: function() {
    		storage.remove('jobViewData');
    	}
    };

    var jobData = {
    	get: function(number) {
    		return storage.get('job' + number, {});
    	},

    	add: function(job) {
    		storage.set('job' + job.number, {"table": job.table});
    	},

    	has: function(job) {
    		return storage.has('job' + job.number);
    	},

    	remove: function(number) {
    		storage.remove('job' + number);
    	}
    }

    var job = {
    	getList: function() {
    		return jobList.getList();
    	},

    	getListWithViewData: function() {
    		var list = jobList.getList();
    		var viewData = jobViewData.getViewData();
    		var buf = [];
    		for (var i = 0; i < list.length; i ++) {
    			var number = list[i];
    			buf.push(viewData[number]);
    		}
    		return buf;
    	},

	    get: function(number) {
	    	return jobData.get(number);
		},

		add: function(job) {
			var list = jobList.getList();
			if (list.indexOf(job.number) >= 0) {
				return;
			}

			var number = job.number;
			list.unshift(number);
            jobList.saveList(list);

            var count = storage.get('jobCount', 0);
            count ++;
            storage.set('jobCount', count);

			var viewData = jobViewData.getViewData();
			viewData[number] = {
                no: count,
                number: job.number,
                name: job.name,
                employmentType: job.employmentType,
                income: job.income,
                place: job.place,
                acceptDate: job.acceptDate,
            };
			jobViewData.saveViewData(viewData);

			jobData.add(job);

            if (list.length > JOB_SAVE_MAX) {
                var num = list.pop();
                this.remove(num);
            }
		},

		has: function(job) {
			return jobData.has(job);
		},

    	remove: function(number) {

    		if (jobData.has({number: number})) {
    			jobData.remove(number);

    			var list = jobList.getList();
    			var index = list.indexOf(number);
    			list.splice(index, 1);
    			jobList.saveList(list);

    			var viewData = jobViewData.getViewData();
    			delete viewData[number];
    			jobViewData.saveViewData(viewData);
    		}
    	},

    	removeAll: function() {
    		var list = jobList.getList();
    		
    		for (var i = 0; i < list.length; i++) {
    			var number = list[i];
    			jobData.remove(number);
    		}

    		jobList.remove();
    		jobViewData.remove();
    	},

    	getCount: function() {
    		return jobList.getList().length;
    	}
    };

    var searchList = {
        get: function() {
            return storage.get('searchList', []);
        },

        save: function(searchList) {
            storage.set('searchList', searchList);
        },

        remove: function() {
            storage.remove('searchList');
        },
    };

    var search = {
        add: function(search) {
            var list = searchList.get();

            var searchCount = storage.get('searchCount', 0);
            searchCount ++;
            storage.set('searchCount', searchCount);

            search.id = searchCount;
            search.created = (new Date).getTime();

            list.unshift(search);
            searchList.save(list);

            while (list.length > SEARCH_SAVE_MAX) {
                var item = list.pop();
                this.remove(item.id);
            }
        },

        get: function(id) {
            var list = searchList.get();
            var ids = list.map(function(x) { return x.id; });
            var index = ids.indexOf(Number(id));
            if (index >= 0) {
                return list[index];
            }
            return null;
        },

        getList: function() {
            return searchList.get();
        },

        remove: function(id) {
            var list = searchList.get();
            var ids = list.map(function(x) { return x.id; });
            var index = ids.indexOf(id);
            if (index >= 0) {
                list.splice(index, 1);
                searchList.save(list);
            }
        },
    };

    window.storage = storage;
    window.job = job;
    window.search = search;
})();